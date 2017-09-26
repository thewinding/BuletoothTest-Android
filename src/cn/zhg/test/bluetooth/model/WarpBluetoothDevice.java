/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月15日 上午11:47:03
 */
package cn.zhg.test.bluetooth.model;

import android.bluetooth.BluetoothDevice;

/**
 * 扩展蓝牙设备
 *
 */
public class WarpBluetoothDevice
{
	private BluetoothDevice mDevice;
	private Short mRssi;

	public WarpBluetoothDevice(BluetoothDevice device, Short rssi)
	{
		mDevice = device;
		mRssi = rssi;
	}

	/**
	 * @return mDevice
	 */
	public BluetoothDevice getDevice()
	{
		return mDevice;
	}

	/**
	 * @return mRssi
	 */
	public Short getRssi()
	{
		return mRssi;
	}

	@Override
	public int hashCode()
	{
		return mDevice.hashCode();
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
		if (obj instanceof WarpBluetoothDevice)
		{
			WarpBluetoothDevice that = (WarpBluetoothDevice) obj;
			return mDevice.equals(that.mDevice);
		}
		return false;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		if (mDevice.getName() != null)
		{
			sb.append(mDevice.getName() + "[" + mDevice.getAddress() + "]");
		} else
		{
			sb.append(mDevice.getAddress());
		}
		if (this.mRssi != null)
		{
			sb.append("(" + mRssi + ")");
		}
		return sb.toString();
	}

}
