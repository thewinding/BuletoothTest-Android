package cn.zhg.test.bluetooth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	public void openRfcommServer(View view)
	{
		this.startActivity(new Intent(this,RfcommServerActivity.class));
	}
	public void openRfcommClient(View view)
	{
		this.startActivity(new Intent(this,RfcommClientActivity.class));
	}
	public void openGattServer(View view)
	{
		this.startActivity(new Intent(this,GattServerActivity.class));
	}
	public void openGattClient(View view)
	{
		this.startActivity(new Intent(this,GattClientActivity.class));
	}
}
