/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月26日 下午6:26:14
 */
package cn.zhg.test.bluetooth;

import java.util.List;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cn.zhg.base.BasicActivity;
import cn.zhg.test.bluetooth.adapter.*;
import cn.zhg.test.bluetooth.model.WarpUUID;
import xiaogen.util.Logger;

/**
 * 
 *低功耗蓝牙客户端
 */
@SuppressWarnings("deprecation")
public class GattClientActivity extends BasicActivity implements Constants, OnClickListener
{ 

	private BluetoothDevice device;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mGatt;
	
	private TextView msgText;
	private TextView sendText;
	private View sendBtn;  
	private   ListView listV;
	private ScrollView scrollV;
	
	private BluetoothGattCallback mConnectCallback= new BluetoothGattCallback()
	{ 
		
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState)
		{
			Logger.d("连接状态变化:" + status + "," + newState); 
			if (newState == BluetoothProfile.STATE_CONNECTED)
			{
				mGatt = gatt;
				info("连接上服务器",gatt.getDevice() );   
				gatt.discoverServices();
			 
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED)
			{
				mGatt = null;
				error("断开服务器" ); 
			} 
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			Logger.d("发现新服务,status=" + status);
			if (status == BluetoothGatt.GATT_SUCCESS)
			{ 
				notifyServicesChanged(); 
			}
		}

		/**
		 * {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic)} 服务器回调
		 */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status)
		{
			Logger.d("characteristic="+characteristic);
			if(status!=BluetoothGatt.GATT_SUCCESS)
			{ 
				error("Characteristic{"+characteristic.getUuid()+"} 发送失败:"+new String(characteristic.getValue()));
			}
		}
		/**
		 * {@link android.bluetooth.BluetoothGattServer#notifyCharacteristicChanged }
		 *   
		 *  服务器通知
		 */
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic)
		{
			Logger.d("characteristic="+characteristic);
			info("服务端",new String(characteristic.getValue()));
		}
		/**
		 *  {@link BluetoothGatt#writeDescriptor(BluetoothGattDescriptor)} 服务器回调
		 */
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt,
				BluetoothGattDescriptor descriptor, int status)
		{
			Logger.d("descriptor=" + descriptor);
			if(status!=BluetoothGatt.GATT_SUCCESS)
			{ 
				error("Descriptor{"+descriptor.getUuid()+"} 发送失败:"+new String(descriptor.getValue()));
			}
		}

	};
	private Point displaySize;;
	
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gatt_client);
		// check it
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
		this.checkPermission();

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
		listV.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);  
	}
	 
	/**
	 * 刷新服务列表
	 */
	private void notifyServicesChanged()
	{
		info("发现新服务"); 
		this.setCharacteristicNotification();
		Runnable action = new Runnable()
		{
			@Override
			public void run()
			{
				listV.setAdapter(new MyListAdapter(GattClientActivity.this,
						listV, mGatt.getServices()));
			}
		};
		listV.post(action);
	}

	/**
	 * 设置服务权限
	 */
	private void setCharacteristicNotification()
	{
		List<BluetoothGattService> servers = mGatt.getServices();
		if (servers != null)
		{
			for (BluetoothGattService server : servers)
			{
				List<BluetoothGattCharacteristic> chs = server
						.getCharacteristics();
				if (chs != null)
				{
					for (BluetoothGattCharacteristic ch : chs)
					{
						if ((ch.getProperties()
								& BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
						{ 
							mGatt.setCharacteristicNotification(ch, true);
						}
					}
				}
			}
		}
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
		if (!this.isConnect())
		{
			tip("未连接");
			return;
		}
		int position = listV.getCheckedItemPosition();
		// Logger.d("当前选中:" + position);
		if (position == -1)
		{
			tip("请先选择一种服务");
			return;
		}
		WarpUUID item = (WarpUUID) listV.getItemAtPosition(position);
		// Logger.d("item=" + item); 
		if (v == this.sendBtn)
		{
			String msg = sendText.getText().toString();
			// info("发送服务", item.getService().getUuid()); 
			info("发送 "+item.getService().getUuid(), msg);
			if (item.isDescriptor())
			{
				BluetoothGattDescriptor de = item.getDescriptor();
				de.setValue(msg.getBytes());
				mGatt.writeDescriptor(de);
			} else
			{
				BluetoothGattCharacteristic ch = item.getCharacteristic();
				ch.setValue(msg.getBytes());
				mGatt.writeCharacteristic(ch);
			}
		}  
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.gatt_client, menu); 
		return true;
	}
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean isConnect=isConnect();
		menu.findItem(R.id.action_connect).setChecked(isConnect);
		if(!isConnect&&this.device!=null )
		{
			menu.findItem(R.id.action_reconnect).setVisible(true);
		}else
		{
			menu.findItem(R.id.action_reconnect).setVisible(false);
		}
		menu.findItem(R.id.action_services).setChecked(isShowListView());
		return super.onPrepareOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.action_connect:
			if(item.isChecked())
			{
				this.disconnect();
			}else
			{
				//选择设备
				Intent intent=new Intent(this,LeDevicesListActivity.class);
				this.startActivityForResult(intent, REQUEST_SELECT_DEVICE);
			}
			return true;
		case R.id.action_reconnect:
			this.connect();
			return true;
		case R.id.action_services: 
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

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SELECT_DEVICE)
		{
			if (resultCode == RESULT_OK)
			{
				this.clear();
				// 连接
				ScanResult scanResult = data
						.getParcelableExtra(EXTRA_SCAN_RESULT);
				ScanRecord record;
				if (scanResult != null)
				{
					this.device = scanResult.getDevice();
					record = scanResult.getScanRecord();  
					info("连接设备",device.getName());
					info("信号强度",scanResult.getRssi());
					info("ServiceUuids",record.getServiceUuids());
					info("ServiceData",record.getServiceData());
					Logger.d("ServiceUuids = "+record.getServiceUuids());
					Logger.d("ServiceData = "+record.getServiceData());
				} else
				{
					this.device = data
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				}
				if (device == null)
				{
					tip("未选择设备!");
					return;
				}
				this.connect();
			} else
			{
				tip("用户取消");
			}
		}
	}
	/**
	 * @return
	 */
	private boolean isConnect()
	{
		return mGatt!=null;
	}

	/**
	 * 
	 */
	private void connect()
	{
		if(!this.mBluetoothAdapter.isEnabled())
		{
			tip("请开启蓝牙后,再重新连接");
			return;
		}
		info("正在连接设备" + device.getName()+"["+device.getAddress()+"]");
		mGatt=device.connectGatt(this, false, mConnectCallback);
	}

	/**
	 * 
	 */
	private void disconnect()
	{
		if(mGatt!=null)
		{
			info("断开连接");
			mGatt.disconnect();
			mGatt=null;
		}
	}

	protected void onDestroy()
	{
		this.disconnect();

		if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
		{
			mBluetoothAdapter.disable();
		}
		super.onDestroy();
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
	private void info(final String tag, final Object msg)
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
