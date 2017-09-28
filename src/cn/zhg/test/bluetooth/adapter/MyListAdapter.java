/**
 * 
 * @author zhhaogen
 * 创建于 2017年9月26日 下午10:12:02
 */
package cn.zhg.test.bluetooth.adapter;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TwoLineListItem;
import cn.zhg.test.bluetooth.model.WarpUUID;

/**
 * 
 *UUID元素适配器
 */
@SuppressWarnings("deprecation")
public class MyListAdapter extends BaseAdapter
{
	private Context context;
	private List<WarpUUID> datas;
	private LayoutInflater inflater; 
	@SuppressWarnings("unused")
	private AbsListView listV;
	public MyListAdapter(Context mContext,AbsListView mListV,
			List<BluetoothGattService> mServices)
	{
		this.context = mContext;
		this.listV=mListV;
		inflater = LayoutInflater.from(context);
		if (mServices != null)
		{
			 this.datas=WarpUUID.buildFromGattServices(mServices); 
		} else
		{
			datas = new ArrayList<>();
		} 
	}
	
	@Override
	public int getCount()
	{
		return datas.size();
	}
 
	@Override
	public WarpUUID getItem(int position)
	{
		return datas.get(position);
	}
 
	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		WarpUUID item = this.getItem(position);
		if (convertView == null)
		{
			convertView = inflater.inflate(
					android.R.layout.simple_list_item_activated_2, parent,
					false); 
		} 
		TwoLineListItem itemView=(TwoLineListItem) convertView;
		 itemView.getText1().setText(item.getType());
		 itemView.getText2().setText(item.getUuid()+"");
		return convertView;
	}

}
