/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月22日 下午4:01:47
 */
package cn.zhg.test.bluetooth;

import java.util.UUID;

/**
 * 一些常量
 *
 */
public interface Constants
{
	int REQUEST_SELECT_DEVICE = 20;
	int REQUEST_ENABLE_BT = 21;
	int REQUEST_SELECT_UUID = 22;
	String SERVER_NAME="TEST";
	UUID OBEXObject_UUID=UUID.fromString("00001105-0000-1000-8000-00805f9b34fb");
	UUID SERVER_UUID=UUID.fromString("00000000-0000-0000-8ac5-7d35847d5158");;
}
