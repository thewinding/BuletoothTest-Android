/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月26日 下午10:13:34
 */
package cn.zhg.test.bluetooth.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

/**
 * BluetoothGattCharacteristic或者BluetoothGattDescriptor
 */
public class WarpUUID
{

	private BluetoothGattCharacteristic characteristic;
	private BluetoothGattDescriptor descriptor;

	/**
	 * 
	 */
	public WarpUUID(BluetoothGattCharacteristic characteristic)
	{
		this.characteristic=characteristic;
	}

	/**
	 * 
	 */
	public WarpUUID(BluetoothGattDescriptor descriptor)
	{
		this.descriptor=descriptor;
	}

	/**
	 * @return characteristic
	 */
	public BluetoothGattCharacteristic getCharacteristic()
	{
		if(characteristic!=null)
		{
			return characteristic ;
		}
		if(descriptor!=null)
		{
			return descriptor.getCharacteristic() ;
		}
		return null;
	}
	public BluetoothGattService getService()
	{
		if(characteristic!=null)
		{
			return characteristic.getService();
		}
		if(descriptor!=null)
		{
			return descriptor.getCharacteristic().getService();
		}
		return null;
	} 
	/**
	 * 是否携带BluetoothGattDescriptor
	 * @return
	 */
	public boolean isDescriptor()
	{
		return descriptor!=null;
	}
	/**
	 * @return descriptor
	 */
	public BluetoothGattDescriptor getDescriptor()
	{
		return descriptor;
	}
	/**
	 * @return
	 */
	public UUID getUuid()
	{
		if(characteristic!=null)
		{
			return characteristic.getUuid();
		}
		if(descriptor!=null)
		{
			return descriptor.getUuid();
		}
		return null;
	}
	public String getType()
	{
		if(characteristic!=null)
		{
			return "Characteristic";
		}
		if(descriptor!=null)
		{
			return "Descriptor";
		}
		return null;
	}
	@Override
	public String toString()
	{ 
		if(characteristic!=null)
		{
			return "Characteristic{"+characteristic.getUuid()+"}";
		}
		if(descriptor!=null)
		{
			return "Descriptor{"+descriptor.getUuid()+"}";
			 
		}
		return null;
	}
	/**
	 * @param services
	 * @return 
	 * @return
	 */
	public static List<WarpUUID> buildFromGattServices(List<BluetoothGattService> services)
	{ 
		 List<WarpUUID> datas = new ArrayList<>();
		for(BluetoothGattService service:services)
		{
			List<BluetoothGattCharacteristic> chs = service.getCharacteristics();
			if(chs!=null)
			{
				for(BluetoothGattCharacteristic ch:chs)
				{
					datas.add(new WarpUUID(ch));
					List<BluetoothGattDescriptor> des = ch.getDescriptors();
					if(des!=null)
					{
						for(BluetoothGattDescriptor de:des)
						{
							datas.add(new WarpUUID(de));
						}
					}
				}
			}
		}
		return datas;
	} 
}
