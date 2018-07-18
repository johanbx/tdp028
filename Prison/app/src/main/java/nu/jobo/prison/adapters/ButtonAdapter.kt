package nu.jobo.prison.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button

class ButtonAdapter(private val mContext: Context, private val buttons: Array<Button>) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val buttonView: View
        if (convertView != null) {
            return convertView
        }

        return buttons[position]
    }

    override fun getCount(): Int = buttons.size

    override fun getItem(position: Int): Any? = buttons[position]

    override fun getItemId(position: Int): Long = buttons[position].id.toLong()

}