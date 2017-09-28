/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月26日 下午6:26:14
 */
package cn.zhg.test.bluetooth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.zhg.base.BaseActivity;
import cn.zhg.test.bluetooth.model.WarpBluetoothDevice;
import xiaogen.util.Logger;

/**
 * 
 *低功耗蓝牙服务端
 */
@SuppressWarnings("deprecation")
public class GattServerActivity extends BaseActivity implements Constants, OnClickListener
{
	private TextView msgText;
	private TextView sendText;
	private View sendBtn; 
	private   ListView listV;
	private ScrollView scrollV;
	
	private Set<WarpBluetoothDevice> devices;
	
	private BluetoothAdapter mBluetoothAdapter;
	private Point displaySize;
	private BluetoothManager bluetoothManager;
	private BluetoothGattServerCallback mConnectCallback=new BluetoothGattServerCallback(){
		
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status,
				int newState)
		{ 
			 Logger.d("设备:"+device+",status="+status+",newState="+newState);
			 if(newState==BluetoothProfile.STATE_CONNECTED)
			 {
				 WarpBluetoothDevice _device = new WarpBluetoothDevice(device,null);
				 info("新连接",_device);   
				 devices.add(_device);
				 notifyDevicesChanged();
			 }else  if(newState==BluetoothProfile.STATE_DISCONNECTED)
			 {
				 WarpBluetoothDevice _device = new WarpBluetoothDevice(device,null);
				 info("断开连接",_device);
				 mServer.cancelConnection(device);
				 devices.remove(_device);
				 notifyDevicesChanged(); 
			 }
			
		} 
		@Override
		public void onServiceAdded(int status, BluetoothGattService service)
		{
			if (status == BluetoothGatt.GATT_SUCCESS)
			{
				info("添加服务成功", service.getUuid()); 
			}else
			{
				info("添加服务失败", service.getUuid());
			} 
			 
		}
 
		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device,
				int requestId, BluetoothGattCharacteristic characteristic,
				boolean preparedWrite, boolean responseNeeded, int offset,
				byte[] value)
		{
			Logger.d(" device=" + device + ",characteristic=" + characteristic);
			//来自客户端写入
			info(device.getName()+device.getAddress(),   new String(value));
			//通知客户端读写成功
			mServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
					offset, value);
		}
	};
	private AdvertiseCallback advertiseCallback=new AdvertiseCallback(){
 
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect)
		{ 
			Logger.d("settingsInEffect="+settingsInEffect );
			info("发布服务成功");
		}
 
		@Override
		public void onStartFailure(int errorCode)
		{ 
			Logger.d("errorCode="+errorCode);
			error("发布服务失败:"+errorCode);
			disconnect();
		}
		
	};
	private BluetoothGattServer mServer;
	private BluetoothLeAdvertiser mLeAdvertiser;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gatt_server);
		// check it
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
		if (!mBluetoothAdapter.isEnabled())
		{
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		}
		
		devices=new HashSet<>();
		// init view
		msgText = this.getViewById(R.id.msgText);
		sendText = this.getViewById(R.id.sendText);
		sendBtn = this.getViewById(R.id.sendBtn);
		sendBtn.setOnClickListener(this);
		scrollV = this.getViewById(R.id.scrollV);
		listV = this.getViewById(R.id.listV);
		displaySize = new Point();
		this.getWindowManager().getDefaultDisplay().getSize(displaySize);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) listV
				.getLayoutParams();
		lp.width = (int) (displaySize.x * 0.7f);
		lp.setMargins(-lp.width, 0, 0, 0);
		listV.setLayoutParams(lp);
		listV.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);  
	}
	/**
	 * 隐藏客户端列表
	 */
	private void hideListView()
	{
		scrollV.setBackgroundColor(Color.WHITE);
		ValueAnimator animation = ObjectAnimator.ofFloat(listV, "X",0,  - (displaySize.x * 0.7f)); 
		animation.start();
	}
	/**
	 *显示客户端列表
	 */
	private void showListView()
	{ 
		scrollV.setBackgroundResource(R.color.colorPrimaryDark);
		ValueAnimator animation = ObjectAnimator.ofFloat(listV, "X",  - (displaySize.x * 0.7f),0); 
		animation.start();
	}
	/**
	 * 是否在显示列表
	 * @return
	 */
	private boolean isShowListView()
	{ 
		return listV.getX()==0;
	}
	@Override
	public void onBackPressed()
	{
		if(this.isShowListView())
		{
			this.hideListView();
		}else
		{
			super.onBackPressed();
		} 
	}
	@Override
	public void onClick(View v)
	{
		if(!this.isConnect())
		{
			tip("未启动");
			return;
		}
		SparseBooleanArray positions = listV.getCheckedItemPositions();
//		Logger.d("当前选中:" + positions);
		List<WarpBluetoothDevice> selectDevices=new ArrayList<>();
		for(int i=0,size=positions.size();i<size;i++)
		{
			int position=positions.keyAt(i);
			if(positions.get(position))
			{
				selectDevices.add( (WarpBluetoothDevice)listV.getItemAtPosition(position));
			}
		}
		if(selectDevices.isEmpty())
		{
			tip("至少选择一个设备");
			return;
		}
		String msg=this.sendText.getText().toString();
		byte[] datas = msg.getBytes();
		BluetoothGattCharacteristic messageCharacteristic =mServer.getService(SERVICE_MESSAGE_UUID).getCharacteristic(CHARACTERISTIC_MESSAGE_UUID);
		messageCharacteristic.setValue(datas);
		for(WarpBluetoothDevice device :selectDevices)
		{
			Logger.d(device+""); 
			boolean ret=mServer.notifyCharacteristicChanged(device.getDevice(), messageCharacteristic, false); 
			if(ret)
			{
				info(device ,msg);
			}else
			{
				error(device +" :" +msg);
			}
		} 
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.gatt_server, menu); 
		return true;
	}
	public boolean onPrepareOptionsMenu(Menu menu)
	{ 
		menu.findItem(R.id.action_start).setChecked(isConnect());
		menu.findItem(R.id.action_devices).setChecked(isShowListView());
		return super.onPrepareOptionsMenu(menu);
	}
	protected void onDestroy()
	{
		this.disconnect(); 
		if(this.mBluetoothAdapter!=null)
		{
			mBluetoothAdapter.disable();
		}
		super.onDestroy();
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.action_start:
			if (item.isChecked())
			{
				this.disconnect();
			} else
			{
				this.startServer();
			}
			return true;
		case R.id.action_devices: 
			if (item.isChecked())
			{
				this.hideListView();
			} else
			{
				this.showListView(); 
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	/**
	 * 刷新客户端列表
	 */
	private void notifyDevicesChanged()
	{
		 Logger.d("services = "+mServer.getServices() +",Devices="+bluetoothManager.getConnectedDevices(BluetoothGattServer.GATT) );
		 Runnable action=new Runnable(){ 
			@Override
			public void run()
			{
				ArrayAdapter<WarpBluetoothDevice> adapter = new ArrayAdapter<>(
						GattServerActivity.this,
						android.R.layout.simple_list_item_multiple_choice,
						new ArrayList<>(devices));
				listV.setAdapter(adapter);
			}};
		listV.post(action);
	
	}
	/**
	 * @return
	 */
	private boolean isConnect()
	{
		return mServer!=null;
	}
	/**
	 * 
	 */
	private void startServer()
	{
		if(!this.mBluetoothAdapter.isEnabled())
		{
			tip("请开启蓝牙后,再重试");
			return;
		}
		this.clear();
		info("正在开启服务....");
		mServer=bluetoothManager.openGattServer(this, this.mConnectCallback);   
		addServices();
		startAdvertising();
	}

	/**
	 * 发布服务
	 */
	private void startAdvertising()
	{
		info("正在发布服务...");
		mLeAdvertiser = this.mBluetoothAdapter.getBluetoothLeAdvertiser();
		if (mLeAdvertiser == null)
		{
			error("设备不支持发布服务,不能作为服务端");
			this.disconnect();
			return;
		}
		mLeAdvertiser.stopAdvertising(advertiseCallback);

		AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
		// 或者设为ADVERTISE_MODE_LOW_LATENCY,提高搜索率
		settingsBuilder
		.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		settingsBuilder.setTimeout(0);

		AdvertiseData.Builder advertiseDataBuilder = new AdvertiseData.Builder();
		advertiseDataBuilder.addServiceUuid(new ParcelUuid(SERVICE_MESSAGE_UUID));
		advertiseDataBuilder.setIncludeDeviceName(true);

		mLeAdvertiser.startAdvertising(settingsBuilder.build(),
				advertiseDataBuilder.build(), advertiseCallback);
	}

	/**
	 * 
	 */
	private void addServices()
	{
		info(null, "正在添加服务...");

		BluetoothGattService messageService = new BluetoothGattService(
				SERVICE_MESSAGE_UUID,
				BluetoothGattService.SERVICE_TYPE_PRIMARY);

		BluetoothGattCharacteristic messageCharacteristic = new BluetoothGattCharacteristic(
				CHARACTERISTIC_MESSAGE_UUID,
				BluetoothGattCharacteristic.PROPERTY_READ
						| BluetoothGattCharacteristic.PROPERTY_WRITE
						| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
				BluetoothGattCharacteristic.PERMISSION_READ
						| BluetoothGattCharacteristic.PERMISSION_WRITE);
		messageService.addCharacteristic(messageCharacteristic);
		mServer.clearServices();
		mServer.addService(messageService); 
	}
	/**
	 * 
	 */
	private void disconnect()
	{
		if(mServer!=null)
		{
			info("停止服务");
			mServer.close();
			mServer=null;
		} 
		if (mLeAdvertiser != null)
		{
			mLeAdvertiser.stopAdvertising(advertiseCallback);
		}
	}
	private void clear( )
	{
		msgText.setText("");
	}
	/**
	 * 提示消息
	 * 
	 * @param msg
	 */
	private void info(  final Object msg)
	{
		msgText.post(new Runnable()
		{ 
			public void run()
			{

				msgText.append(Html.fromHtml(
						"<font color=\"#0000ff\">" + msg + "</font><br>\n")); 
			}
		});
	}

	/**
	 * 提示消息
	 * 
	 * @param msg
	 */
	private void info(final Object tag, final Object msg)
	{
		msgText.post(new Runnable()
		{
			public void run()
			{
				if (tag != null)
				{
					msgText
							.append(Html.fromHtml("<font color=\"#34B4ED\">"
									+ tag + "</font> : <font color=\"#0000ff\">"
									+ msg + "</font><br>\n"));
				} else
				{
					msgText
							.append(Html.fromHtml("<font color=\"#0000ff\">"
									+ msg + "</font><br>\n"));
				}
			}
		});
	}

	/**
	 * 错误消息
	 * @param msg
	 */
	private void error(final Object msg)
	{
		msgText.post(new Runnable()
		{ 
			public void run()
			{
				msgText.append(Html.fromHtml(
						"<font color=\"#EA892F\">" + msg + "</font><br>\n"));
			}
		});
	}
}
