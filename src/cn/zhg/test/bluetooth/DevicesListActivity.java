/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月22日 下午3:48:28
 */
package cn.zhg.test.bluetooth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import cn.zhg.base.BaseActivity;
import cn.zhg.test.bluetooth.model.WarpBluetoothDevice;
import xiaogen.util.Logger; 
/**
 * 
 *搜索设备
 */
public class DevicesListActivity extends BaseActivity implements OnItemClickListener,Constants
{
	private Set<WarpBluetoothDevice> devices;
	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mReceiver;
	private ListView listV;

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_list);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			tip("不支持蓝牙");
			this.setResult(RESULT_CANCELED);
			finish();
			return;
		}
		listV = this.getViewById(R.id.listV);
		listV.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
		devices = new HashSet<>();
		
		registerReceiver();

		startDiscovery();

		Set<BluetoothDevice> mBondedDevices = mBluetoothAdapter
				.getBondedDevices();
		Logger.d("已配对设备:" + mBondedDevices);
		if (mBondedDevices != null)
		{
			for (BluetoothDevice b : mBondedDevices)
			{
				devices.add(new WarpBluetoothDevice(b, null));
			}
			notifyDataSetChanged();
		}
		listV.setOnItemClickListener(this);
	}
	/**
	 * 开始搜索
	 */
	private void startDiscovery()
	{
		if (!mBluetoothAdapter.isEnabled())
		{
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		} else
		{
			mBluetoothAdapter.startDiscovery();// 开启搜索
		}
	}
	/**
	 * 注册广播监听器
	 */
	private void registerReceiver()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_CLASS_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED); 
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
							mBluetoothAdapter.startDiscovery();
						}
					} else if (BluetoothDevice.ACTION_FOUND.equals(action)
							|| BluetoothDevice.ACTION_CLASS_CHANGED
									.equals(action)
							|| BluetoothDevice.ACTION_NAME_CHANGED
									.equals(action))
					{
						BluetoothDevice device = data
								.getParcelable(BluetoothDevice.EXTRA_DEVICE);
						Short ssid = null;
						if (data.containsKey(BluetoothDevice.EXTRA_RSSI))
						{
							ssid = data.getShort(BluetoothDevice.EXTRA_RSSI);
						}
						WarpBluetoothDevice _device = new WarpBluetoothDevice(
								device, ssid); 
						addDevice(_device);
					}  
				}
			}
		};
		this.registerReceiver(mReceiver, filter);
	}
	/**
	 * @param _device
	 */
	private void addDevice(WarpBluetoothDevice _device)
	{
		devices.add(_device);
		notifyDataSetChanged();
	}

	private void notifyDataSetChanged()
	{
		ArrayAdapter<WarpBluetoothDevice> adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_single_choice,
				new ArrayList<>(this.devices)); 
		listV.setAdapter(adapter);
	}
	 
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		WarpBluetoothDevice devices = (WarpBluetoothDevice) parent.getAdapter()
				.getItem(position);
		Intent data=this.getIntent();
		if(data==null)
		{
			data=new Intent();
		}
		data.putExtra(BluetoothDevice.EXTRA_DEVICE, devices.getDevice());
		this.setResult(RESULT_OK, data);
		this.finish();
	}
	protected void onDestroy()
	{
		if (mReceiver != null)
		{
			this.unregisterReceiver(mReceiver);
		}
		if (mBluetoothAdapter != null)
		{
			mBluetoothAdapter.cancelDiscovery();
		}
		super.onDestroy();
	}
}
