package com.example.appblocker.adapter



import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.appblocker.R

class AppListAdapter(private val context: Context, private val apps: List<ApplicationInfo>) : BaseAdapter() {

    private val packageManager: PackageManager = context.packageManager

    override fun getCount(): Int = apps.size

    override fun getItem(position: Int): Any = apps[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false)

        val app = apps[position]
        val appNameTextView = view.findViewById<TextView>(R.id.app_name)
        val appIconImageView = view.findViewById<ImageView>(R.id.app_icon)

        appNameTextView.text = packageManager.getApplicationLabel(app)
        appIconImageView.setImageDrawable(packageManager.getApplicationIcon(app))

        return view
    }
}
