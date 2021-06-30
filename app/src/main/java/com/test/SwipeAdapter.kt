package com.test

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.test.databinding.ItemRcvSwipeContentBinding
import com.test.databinding.ItemRcvSwipeOptionBinding

class SwipeAdapter(recyclerView: RecyclerView) : SwipeOptionAdapter<SwipeAdapter.ContentViewHolder, SwipeAdapter.OptionViewHolder>(recyclerView) {
    private val mContext = recyclerView.context

    override fun onCreateViewHolderContent(parent: ViewGroup, viewType: Int): ContentViewHolder {
        return ContentViewHolder(ItemRcvSwipeContentBinding.inflate(LayoutInflater.from(mContext), parent, false))
    }

    override fun onCreateViewHolderOption(parent: ViewGroup, viewType: Int): OptionViewHolder {
        return OptionViewHolder(ItemRcvSwipeOptionBinding.inflate(LayoutInflater.from(mContext), parent, false))
    }

    override fun onBindViewHolderContent(holder: ContentViewHolder, position: Int) {
        holder.view.tvName.text = MainActivity.DATA.random()
        holder.view.tvDesc.text = MainActivity.DATA.random()
    }

    override fun onBindViewHolderOption(holder: OptionViewHolder, position: Int) {
        holder.view.viewEdit.setOnClickListener {
            Toast.makeText(mContext, "Edited", Toast.LENGTH_SHORT).show()
        }
        holder.view.viewDelete.setOnClickListener {
            Toast.makeText(mContext, "Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getOptionViewWidth(): Int {
        return mContext.resources.getDimensionPixelSize(R.dimen.item_option_width)
    }

    override fun getItemCount(): Int {
        return 20
    }

    class ContentViewHolder(val view: ItemRcvSwipeContentBinding) : RecyclerView.ViewHolder(view.root)
    class OptionViewHolder(val view: ItemRcvSwipeOptionBinding) : RecyclerView.ViewHolder(view.root)
}