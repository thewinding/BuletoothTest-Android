package cn.zhg.test.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import cn.zhg.base.BaseActivity;
import xiaogen.util.Logger; 
/**
 * 
 * 传统客户端
 *
 */
@SuppressWarnings("deprecation")
public class RfcommClientActivity extends BaseActivity implements Constants, OnClickListener
{ 
	private BluetoothAdapter mBluetoothAdapter;
	private TextView msgText;
	private TextView sendText;
	private View sendBtn;
	private BluetoothDevice device;
	private ParcelUuid uuid;
	/**
	 * 蓝牙socket连接
	 */
	private BluetoothSocket socket;
	private DataOutputStream os;
	private boolean isStop;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rfcomm_client);
		msgText=this.getViewById(R.id.msgText);
		sendText=this.getViewById(R.id.sendText);
		sendBtn=this.getViewById(R.id.sendBtn);
		sendBtn.setOnClickListener(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			tip("不支持蓝牙");
			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.rfcomm_client, menu); 
		return true;
	}
	 
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_SELECT_DEVICE)
		{
			if (resultCode == RESULT_OK)
			{
				// 选择uuid
				Intent intent = new Intent(this, DeviceUuidsListActivity.class);
				intent.putExtras(data);
				this.startActivityForResult(intent, REQUEST_SELECT_UUID);
			} else
			{
				tip("用户取消");
			}
		} else if (requestCode == REQUEST_SELECT_UUID)
		{
			if (resultCode == RESULT_OK)
			{
				device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				uuid = data.getParcelableExtra(BluetoothDevice.EXTRA_UUID);
				Logger.d("device =" + device + " ,uuid=" + uuid);
				connect(); 
			} else
			{
				tip("用户取消");
			}
		}
	}

	public boolean onPrepareOptionsMenu(Menu menu)
	{
		boolean isConnect=isConnect();
		menu.findItem(R.id.action_connect).setChecked(isConnect);
		if(!isConnect&&this.device!=null&&this.uuid!=null)
		{
			menu.findItem(R.id.action_reconnect).setVisible(true);
		}else
		{
			menu.findItem(R.id.action_reconnect).setVisible(false);
		}
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
				Intent intent=new Intent(this,DevicesListActivity.class);
				this.startActivityForResult(intent, REQUEST_SELECT_DEVICE);
			}
			return true;
		case R.id.action_reconnect:
			this.connect();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * 开始连接
	 */
	private void connect()
	{
		clear();
		info("正在连接到" + device.getName() + "...");
		info("服务端mac 地址", device.getAddress());
		info("服务端UUID", uuid);
		try
		{
			socket = device
					.createInsecureRfcommSocketToServiceRecord(uuid.getUuid());
			new Thread()
			{  
				public void run()
				{
					try
					{
						isStop = false;
						Logger.d("正在连接...");
						socket.connect();
						Logger.d("连接成功!");
						OutputStream _os = socket.getOutputStream();
						os = new DataOutputStream(_os);
						DataInputStream is = new DataInputStream(
								socket.getInputStream());
						info("连接成功");
						while (!isStop)
						{ 
							int size =is.readInt();
							Logger.d("读取长度:" + size);
							byte[] data = new byte[size];
							int leng = is.read(data);// 实际长度
							Logger.d("文本长度:" + leng);
							info("服务端", new String(data, 0, leng));
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					} finally
					{
						disconnect();
					}
				}
			}.start();
		} catch (IOException e)
		{
			e.printStackTrace();
			error("连接失败:" + e.getMessage());
		}
	} 

	/**
	 * 是否已经连接
	 * @return
	 */
	private boolean isConnect()
	{
		return mBluetoothAdapter!=null&&mBluetoothAdapter.isEnabled()&&socket!=null&&os!=null;
	}

	/**
	 * 断开连接
	 */
	private void disconnect()
	{ 
		try
		{
			if(socket!=null)
			{
				info("断开连接");
				socket.close();
				socket=null;
			} 
			if(os!=null)
			{
				os.close();
				os=null;
			}
		} catch (Exception e)
		{
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
	protected void onDestroy()
	{
		isStop=true;
		this.disconnect();
		if(this.mBluetoothAdapter!=null)
		{
			mBluetoothAdapter.disable();
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View view)
	{
		if (this.isConnect())
		{
			final String s = sendText.getText().toString();
			new Thread()
			{
				public void run()
				{
					try
					{
						int length=s.getBytes().length;
						Logger.d("写入长度:"+length);
						os.writeInt(length);
						os.write(s.getBytes());
						os.flush();
						info("客户端",s);
					} catch (IOException e)
					{
						e.printStackTrace();
						error("发送失败:"+s);
					} 
				}
			}.start();
		} else
		{
			tip("未连接");
		}
	}
}
