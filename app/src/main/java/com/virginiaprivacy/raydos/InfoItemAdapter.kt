package com.virginiaprivacy.raydos

import com.virginiaprivacy.raydos.infoitem.InfoItem
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.commit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import splitties.fragments.addToBackStack


/**
 * [RecyclerView.Adapter] that can display a [InfoItem].
 */
@ExperimentalCoroutinesApi
class InfoItemAdapter(
    private val values: List<InfoItem>, private val context: Context)
    : RecyclerView.Adapter<InfoItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
        if (viewType == R.layout.info_item) {
            LayoutInflater.from(parent.context).inflate(R.layout.info_item, parent, false)
        }
        else {
            LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false)
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
            holder.button.text = context.getString(R.string.info_page_continue_button)
            holder.button.setOnClickListener {
                (context as MainActivity).supportFragmentManager.commit {
                    addToBackStack()
                    replace(R.id.fragment_container_view, if (context.readyFragment != null) {
                        context.readyFragment!!
                    }
                    else {
                        context.readyFragment = ReadyFragment()
                        context.readyFragment!!
                    })
                }
            }
        }
        else {
            val item = values[position]
            holder.titleView.text = item.title
            holder.detailsView.text = when (item.infoPoints.size) {
                1 -> SpannableString(item.infoPoints[0]).apply { setSpan(getBulletSpan(), 0, length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE) }
                else -> {
                    val span = SpannableStringBuilder()
                    item.infoPoints.forEach { string ->
                        SpannableString(string).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                span.append(string + "\n", getBulletSpan(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
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
            R.layout.info_item_button
        }
        else {
            R.layout.info_item
        }
    }

    override fun getItemCount(): Int = values.size + 1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.item_info_title)
        val detailsView: TextView = view.findViewById(R.id.info_item_details)
        val button: Button = view.findViewById(R.id.info_item_button)

        override fun toString(): String {
            return super.toString() + " '" + detailsView.text + "'"
        }
    }
}