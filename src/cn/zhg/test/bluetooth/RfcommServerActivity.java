/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月22日 下午10:14:04
 */
package cn.zhg.test.bluetooth;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
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
import xiaogen.util.Logger;

/**
 * 
 *传统服务端
 */
@SuppressWarnings("deprecation")
public class RfcommServerActivity extends BaseActivity implements Constants, OnClickListener
{
	private BroadcastReceiver mReceiver;
	private BluetoothAdapter mBluetoothAdapter;
	private TextView msgText;
	private TextView sendText;
	private View sendBtn;
	private ListView listV;
	private ScrollView scrollV;
	/**
	 * 屏幕大小
	 */
	private Point displaySize;
	private BluetoothServerSocket server;
	/**
	 * 已连接的客户端列表
	 */
	private Set<Client> clients;
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rfcomm_server);
		msgText = this.getViewById(R.id.msgText);
		sendText = this.getViewById(R.id.sendText);
		listV = this.getViewById(R.id.listV);
		scrollV = this.getViewById(R.id.scrollV);
		sendBtn = this.getViewById(R.id.sendBtn);
		sendBtn.setOnClickListener(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null)
		{
			tip("不支持蓝牙");
			finish();
			return;
		}
		displaySize = new Point();
		this.getWindowManager().getDefaultDisplay().getSize(displaySize);
		Logger.d("屏幕大小:" + displaySize.x + "*" + displaySize.y);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) listV
				.getLayoutParams();
		lp.width = (int) (displaySize.x * 0.7f);
		lp.setMargins(-lp.width, 0, 0, 0);
		listV.setLayoutParams(lp); 
		listV.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
	 
		clients=new HashSet<>();
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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.rfcomm_server, menu); 
		return true;
	}
	public boolean onPrepareOptionsMenu(Menu menu)
	{ 
		menu.findItem(R.id.action_start).setChecked(isConnect());
		menu.findItem(R.id.action_devices).setChecked(isShowListView());
		return super.onPrepareOptionsMenu(menu);
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
	/**
	 * 执行开启服务动作
	 */
	private void startServer()
	{
		if (!mBluetoothAdapter.isEnabled())
		{
			registerReceiver();
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);
		} else
		{
			 connect();// 开启监听
		}
	}
	/**
	 * 注册广播监听器
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
							 connect();// 开启监听
							 unregisterReceiver(this);//不需要在监听蓝牙打开状态
							 mReceiver=null;
						}
					}  
				}
			}
		};
		this.registerReceiver(mReceiver, filter);
	}

	/**
	 * 开启监听
	 */
	private void connect()
	{
		clear();
		try
		{
			info("正在开启服务[" + SERVER_UUID + "]....");
			server = this.mBluetoothAdapter
					.listenUsingInsecureRfcommWithServiceRecord(SERVER_NAME,
							SERVER_UUID); 
			info("服务已经在运行...");
			new Thread()
			{
				public void run()
				{
					try
					{ 
						while (true)
						{ 
							BluetoothSocket socket = server.accept();    
							BluetoothDevice device = socket.getRemoteDevice();
							info("新客户端",device.getName()+"["+device.getAddress()+"]"); 
							addClient(new Client(socket));
						}
					} catch (IOException e)
					{
						e.printStackTrace();
					}finally
					{
						Logger.d("关闭接收线程");
					}
				}
			}.start();
		} catch (IOException e)
		{
			e.printStackTrace();
			error("开启服务失败:"+e);
			server=null;
		}
	}
	/**
	 * @param client
	 */
	private void addClient(Client client)
	{
		client.start();
		clients.add(client); 
		notifyDataSetChanged();
	
	}
	/**
	 * 
	 */
	private void disconnect()
	{
		if(server!=null)
		{
			info("已停止服务...");
			try
			{
				server.close();
				server=null;
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return
	 */
	private boolean isConnect()
	{
		return server != null;
	}
	@Override
	public void onClick(View v)
	{
		 if(v==this.sendBtn)
		 { 
			 String msg=this.sendText.getText().toString();
			 info("服务端",msg);
			SparseBooleanArray ps = this.listV.getCheckedItemPositions();
			List<Client> arr=new ArrayList<>();
			for(int i=0,size=ps.size();i<size;i++)
			{
				int position=ps.keyAt(i);
				if(ps.get(position))
				{
					arr.add((Client) listV.getItemAtPosition(position));
				}
			}
			Iterator<Client> itor = arr.iterator();
			while(itor.hasNext())
			{
				itor.next().sendMessage(msg);
			}
		 }
	}
	protected void onDestroy()
	{
		this.disconnect();
		if (mReceiver != null)
		{
			this.unregisterReceiver(mReceiver);
		}
		if(this.mBluetoothAdapter!=null)
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
	private void notifyDataSetChanged()
	{
		this.listV.post(new Runnable(){ 
			@Override
			public void run()
			{
				ArrayAdapter<Client> adapter = new ArrayAdapter<>(RfcommServerActivity.this,
						android.R.layout.simple_list_item_multiple_choice,
						new ArrayList<>(clients));
				listV.setAdapter(adapter);
			} 
		});
	
	}
	private class Client  implements Runnable
	{

		private BluetoothDevice device;
		private DataOutputStream os;
		private DataInputStream is;
		private BluetoothSocket socket;

		/**
		 * @throws IOException 
		 * 
		 */
		public Client(BluetoothSocket socket) throws IOException
		{
			this.socket=socket;
			this.device=socket.getRemoteDevice();
			this.os=new DataOutputStream(socket.getOutputStream());
			this.is=new DataInputStream(socket.getInputStream()); 
		}
		void sendMessage(final String msg)  
		{
			new Thread()
			{
				public void run()
				{ 
					try
					{ 
						byte[] data = msg.getBytes();
						Logger.d("写入文本长度:"+data.length);
						os.writeInt(data.length);
						os.write(data);
						os.flush();
					} catch (IOException e)
					{
						e.printStackTrace();
						disploe();
					}
				}
			}.start();
			
		}

		void start()
		{
			new Thread(this).start();
		} 

		@Override
		public void run()
		{
			try
			{
				Logger.d("正在读取内容...");
				while (true)
				{ 
					int size = is.readInt(); 
					Logger.d("读取文本长度:"+size);
					byte[] data = new byte[size];
					int leng = is.read(data);// 
					info(this.device.getName(), new String(data, 0, leng)); 
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			}finally
			{
				disploe();
			}
		}
		/**
		 * 释放客户端
		 */
		private void disploe()
		{
			info(device.getName()+" 已经断开连接..");
			if(socket!=null)
			{
				try
				{
					socket.close();
				} catch ( Exception e)
				{ 
				}
				socket=null;
			}
			if(os!=null)
			{
				try
				{
					os.close();
				} catch ( Exception e)
				{ 
				}
				os=null;
			}
			if(is!=null)
			{
				try
				{
					is.close();
				} catch ( Exception e)
				{ 
				}
				is=null;
			}
			clients.remove(this); 
			notifyDataSetChanged();
		}
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			if (device.getName() != null)
			{
				sb.append(device.getName() + "[" + device.getAddress() + "]");
			} else
			{
				sb.append(device.getAddress());
			} 
			return sb.toString();
		}
		public int hashCode()
		{
			return device.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj == null)
			{
				return false;
			}
			if (obj == this)
			{
				return true;
			}
			if (obj instanceof Client)
			{
				Client that = (Client) obj;
				return device.equals(that.device);
			}
			return false;
		}
	}
}
