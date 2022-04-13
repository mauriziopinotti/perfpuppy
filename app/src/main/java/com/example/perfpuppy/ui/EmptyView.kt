package com.example.perfpuppy.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.perfpuppy.R
import com.example.perfpuppy.databinding.ViewEmptyBinding

class EmptyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val binding = ViewEmptyBinding.inflate(LayoutInflater.from(context), this, true)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.EmptyView)
        try {
            val text = ta.getString(R.styleable.EmptyView_text)
            binding.emptyText.text = text
            binding.imageView.contentDescription = text
        } finally {
            ta.recycle()
        }
    }
}
