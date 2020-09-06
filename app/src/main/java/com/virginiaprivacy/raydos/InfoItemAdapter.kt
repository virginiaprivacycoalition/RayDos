package com.virginiaprivacy.raydos

import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.virginiaprivacy.raydos.infoitem.InfoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class InfoItemAdapter(
    private val values: List<InfoItem>)
    : RecyclerView.Adapter<InfoItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            if (viewType == R.layout.info_item) {
                LayoutInflater.from(parent.context).inflate(R.layout.info_item, parent, false)
            }
            else {
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.info_item_last_page, parent, false)
            }
        return ViewHolder(view)
    }

    private fun getBulletSpan() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        BulletSpan(40, Color.BLACK,
            10)
    }
    else {
        null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == values.size) {

        }
        else {
            val item = values[position]
            holder.titleView?.text = item.title
            holder.detailsView?.text = when (item.infoPoints.size) {
                1 -> SpannableString(item.infoPoints[0]).apply {
                    setSpan(getBulletSpan(),
                        0,
                        length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                }
                else -> {
                    val span = SpannableStringBuilder()
                    item.infoPoints.forEach { string ->
                        SpannableString(string).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                span.append(string + "\n",
                                    getBulletSpan(),
                                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                            }
                        }
                    }
                    span
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == values.size) {
            R.layout.info_item_last_page
        }
        else {
            R.layout.info_item
        }
    }

    override fun getItemCount(): Int = values.size + 1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var titleView: TextView? = view.findViewById(R.id.item_info_title)
        var detailsView: TextView? = view.findViewById(R.id.info_item_details)
        var aboutHeader: TextView? = view.findViewById(R.id.last_page_header)

        override fun toString(): String {
            return super.toString() + " '" + detailsView?.text + "'"
        }
    }
}