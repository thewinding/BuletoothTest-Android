/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月22日 下午3:48:28
 */
package cn.zhg.test.bluetooth;

import java.util.Arrays;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import cn.zhg.base.BaseActivity;
import xiaogen.util.Logger; 
/**
 * 
 *搜索设备的服务列表
 */
public class DeviceUuidsListActivity extends BaseActivity implements OnItemClickListener,Constants
{ 
	private BluetoothAdapter mBluetoothAdapter;
	private BroadcastReceiver mReceiver;
	private ListView listV;
	private BluetoothDevice device;
	private View progressBar;

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
		Intent intent = this.getIntent();
		if (intent == null || !intent.hasExtra(BluetoothDevice.EXTRA_DEVICE))
		{
			tip("未选择蓝牙设备!");
			finish();
			return;
		}
		try
		{
			device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		} catch (Exception igr)
		{

		}
		if (device == null  )
		{
			tip("传入蓝牙设备不正确!");
			finish();
			return;
		}
		this.setTitle(device.getName()+ "[" + device.getAddress() + "]"+" UUID列表");
		this.progressBar=this.getViewById(R.id.proBar);
		listV = this.getViewById(R.id.listV);
		listV.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE); 
		
		registerReceiver(); 
	 
		fetchUuidsWithSdp();
 
		notifyDataSetChanged();
		listV.setOnItemClickListener(this);
	}
	/**
	 * 开启蓝牙查找uuid
	 */
	private void fetchUuidsWithSdp()
	{
		progressBar.setVisibility(View.VISIBLE);
		if (!mBluetoothAdapter.isEnabled())
		{
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		} else
		{
			this.device.fetchUuidsWithSdp();
		}
	}
	/**
	 * 注册广播监听器
	 */
	private void registerReceiver()
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_UUID); 
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
							fetchUuidsWithSdp();
						}
					} else if (BluetoothDevice.ACTION_UUID.equals(action))
					{
						BluetoothDevice device = data
								.getParcelable(BluetoothDevice.EXTRA_DEVICE);
						ParcelUuid[] uuids = device.getUuids();
						Logger.d("设备:" + device.getAddress() + ",uuids="
								+ Arrays.toString(uuids)); 
						tip("发现服务数量:"+(uuids==null?0:uuids.length));
						progressBar.setVisibility(View.GONE);
						notifyDataSetChanged();
					}
				}
			}
		};
		this.registerReceiver(mReceiver, filter);
	}
	private void notifyDataSetChanged()
	{
		ParcelUuid[] uuids = this.device.getUuids();
		if(uuids==null)
		{
			uuids=new ParcelUuid[0];
		}
		ArrayAdapter<ParcelUuid> adapter = new ArrayAdapter<>(this,
				android.R.layout.simple_list_item_single_choice, uuids);
		listV.setAdapter(adapter);
	}
	 
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		ParcelUuid uuid = (ParcelUuid) parent.getAdapter()
				.getItem(position);
		Intent data=this.getIntent();
		if(data==null)
		{
			data=new Intent();
		}
		data.putExtra(BluetoothDevice.EXTRA_UUID, uuid);
		this.setResult(RESULT_OK, data);
		this.finish();
	}
	protected void onDestroy()
	{
		if (mReceiver != null)
		{
			this.unregisterReceiver(mReceiver);
		} 
		super.onDestroy();
	}
}
