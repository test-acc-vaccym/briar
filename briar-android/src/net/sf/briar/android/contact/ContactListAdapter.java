package net.sf.briar.android.contact;

import static android.view.Gravity.CENTER_VERTICAL;
import static android.widget.LinearLayout.HORIZONTAL;

import java.util.ArrayList;

import net.sf.briar.R;
import net.sf.briar.android.widgets.CommonLayoutParams;
import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class ContactListAdapter extends ArrayAdapter<ContactListItem>
implements OnItemClickListener {

	ContactListAdapter(Context ctx) {
		super(ctx, android.R.layout.simple_expandable_list_item_1,
				new ArrayList<ContactListItem>());
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ContactListItem item = getItem(position);
		Context ctx = getContext();
		LinearLayout layout = new LinearLayout(ctx);
		layout.setOrientation(HORIZONTAL);
		layout.setGravity(CENTER_VERTICAL);

		ImageView bulb = new ImageView(ctx);
		bulb.setPadding(10, 10, 10, 10);
		if(item.isConnected()) bulb.setImageResource(R.drawable.green_bulb);
		else bulb.setImageResource(R.drawable.grey_bulb);
		layout.addView(bulb);

		TextView name = new TextView(ctx);
		// Give me all the unused width
		name.setLayoutParams(CommonLayoutParams.WRAP_WRAP_1);
		name.setTextSize(18);
		name.setPadding(0, 10, 10, 10);
		name.setText(item.getName());
		layout.addView(name);

		TextView connected = new TextView(ctx);
		connected.setTextSize(14);
		connected.setPadding(0, 10, 10, 10);
		if(item.isConnected()) {
			connected.setText(R.string.contact_connected);
		} else {
			Resources res = ctx.getResources();
			String format = res.getString(R.string.contact_last_connected);
			long then = item.getLastConnected();
			CharSequence ago = DateUtils.getRelativeTimeSpanString(then);
			connected.setText(Html.fromHtml(String.format(format, ago)));
		}
		layout.addView(connected);

		return layout;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// FIXME: Hook this up to an activity
	}
}