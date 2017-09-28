/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月26日 下午7:05:42
 */
package cn.zhg.test.bluetooth.model;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;

/**
 * 
 *扩展ScanResult
 */
public class WarpScanResult
{
	private BluetoothDevice mDevice;
	private ScanResult mResult;

	/**
	 * 
	 */
	public WarpScanResult(ScanResult result)
	{
		this.mResult=result;
		mDevice=result.getDevice();
	}

	/**
	 * @return
	 */
	public BluetoothDevice getDevice()
	{
		return mDevice;
	}
	public ScanResult getScanResult()
	{
		return this.mResult ;
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
		if (obj instanceof WarpScanResult)
		{
			WarpScanResult that = (WarpScanResult) obj;
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
		sb.append("(" + mResult.getRssi() + ")"); 
		ScanRecord record = mResult.getScanRecord();
		if(record!=null)
		{
			List<ParcelUuid> serviceUuids = record.getServiceUuids();
			if(serviceUuids!=null&&!serviceUuids.isEmpty())
			{
				sb.append("\n");
				sb.append( serviceUuids);
			}
			
		}
		return sb.toString();
	}
}
