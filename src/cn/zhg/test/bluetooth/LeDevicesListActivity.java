/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月22日 下午3:48:28
 */
package cn.zhg.test.bluetooth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import cn.zhg.base.BasicActivity;
import cn.zhg.test.bluetooth.model.WarpScanResult;
import xiaogen.util.Logger; 
/**
 * 
 *搜索低功耗设备
 */
public class LeDevicesListActivity extends BasicActivity implements OnItemClickListener,Constants
{
	/**
	 * 搜索结果
	 */
	private Set<WarpScanResult> devices; 
	
	private BroadcastReceiver mReceiver; 
	private ListView listV;
	/**
	 * 蓝牙设备管理器
	 */
	private BluetoothManager bluetoothManager;
	/**
	 * 蓝牙设备适配器
	 */
	private BluetoothAdapter mBluetoothAdapter;
	/**
	 * 搜索低功耗回调
	 */
	private final ScanCallback callback = new ScanCallback()
	{ 
		@Override
		public void onScanResult(int callbackType, ScanResult result)
		{
			Logger.d("返回结果:" + callbackType + "," + result);
			if (result != null)
			{
				devices.add(new WarpScanResult(result)); 
				notifyDataSetChanged();
			}
		}
 
		@Override
		public void onBatchScanResults(List<ScanResult> results)
		{
			Logger.d("返回结果:" + results);
			if (results != null&&!results.isEmpty())
			{
				for(ScanResult result:results)
				{
					devices.add(new WarpScanResult(result)); 
				} 
				notifyDataSetChanged(); 
			}
		} 
		@Override
		public void onScanFailed(int errorCode)
		{
			tip("扫描失败:" + errorCode);
			Logger.d("扫描失败:" + errorCode);
		}
		
	};
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_list);
		bluetoothManager = (BluetoothManager) getSystemService(
				BLUETOOTH_SERVICE); 
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null)
		{
			tip("不支持蓝牙");
			this.setResult(RESULT_CANCELED);
			finish();
			return;
		}
		if (!getPackageManager()
				.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
		{
			tip("不支持低功耗模式");
			finish();
			return;
		}
		this.checkPermission();
		
		devices = new HashSet<>();
		
		listV = this.getViewById(R.id.listV);
		listV.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		listV.setOnItemClickListener(this);   
		
		registerReceiver();
		startScan();  
	}
	/**
	 * 开始搜索
	 */
	private void startScan()
	{
		if (!mBluetoothAdapter.isEnabled())
		{ 
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		} else
		{    
			BluetoothLeScanner mLeScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
			Logger.d("开始扫描");
			if(mLeScanner!=null)
			{ 
				mLeScanner.startScan(callback);
				mLeScanner.flushPendingScanResults(callback);
			}else
			{
				tip("不支持低功耗扫描");
			}
		}
	}
	/**
	 * 注册广播监听器,在蓝牙开启时扫描设备
	 */
	private void registerReceiver()
	{
		IntentFilter filter = new IntentFilter(); 
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mReceiver = new BroadcastReceiver()
		{
			public void onReceive(Context context, Intent intent)
			{
				if (intent != null)
				{
					String action = intent.getAction(); 
					Bundle data = intent.getExtras();
					if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
					{
						int state = data.getInt(BluetoothAdapter.EXTRA_STATE); 
						if (state == BluetoothAdapter.STATE_ON)
						{
							startScan();
							unregisterReceiver(mReceiver);//不再需要监听
							mReceiver=null;
						}
					}  
				}
			}
		};
		this.registerReceiver(mReceiver, filter);
	}

	/**
	 * 更新列表
	 */
	private void notifyDataSetChanged()
	{
		ArrayAdapter<WarpScanResult> adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_single_choice,
				new ArrayList<>(devices));
		listV.setAdapter(adapter);
	}
	 
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		WarpScanResult devices = (WarpScanResult) parent.getAdapter()
				.getItem(position);
		Intent data=this.getIntent();
		if(data==null)
		{
			data=new Intent();
		}
		data.putExtra(BluetoothDevice.EXTRA_DEVICE, devices.getDevice());
		data.putExtra(EXTRA_SCAN_RESULT, devices.getScanResult());
		this.setResult(RESULT_OK, data);
		this.finish();
	}
	protected void onDestroy()
	{
		if (mReceiver != null)
		{
			this.unregisterReceiver(mReceiver);
		}
		BluetoothLeScanner mLeScanner = this.mBluetoothAdapter.getBluetoothLeScanner();
		if (mLeScanner != null)
		{
			Logger.d("停止扫描");
			mLeScanner.stopScan(callback);
		}
		 
		super.onDestroy();
	}
}
