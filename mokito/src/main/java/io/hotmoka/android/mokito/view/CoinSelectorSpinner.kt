package io.hotmoka.android.mokito.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.TextView
import io.hotmoka.android.mokito.R
import io.hotmoka.beans.Coin
import java.math.BigDecimal
import java.math.BigInteger

class CoinSelectorSpinner constructor(
    context: Context, attrs: AttributeSet
) : androidx.appcompat.widget.AppCompatSpinner(context, attrs) {

    private val selects: Int

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CoinSelectorSpinner,
            0, 0).apply {

            try {
                selects = getResourceId(R.styleable.CoinSelectorSpinner_selects, -1)
            } finally {
                recycle()
            }
        }
    }

    init {
        val adapter = ArrayAdapter.createFromResource(
            context,
            R.array.coin_types_array,
            android.R.layout.simple_spinner_item
        )

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        this.adapter = adapter
    }

    fun asPanareas(): BigInteger {
        if (selects < 0)
            throw IllegalStateException("Add controls attribute to specify the view controlled by the coin selector")

        var parent = this.parent
        while (parent.parent is android.view.View)
            parent = parent.parent

        if (parent is android.view.View) {
            val target: TextView = parent.findViewById(selects)
            val withDecimals = BigDecimal(target.text.toString())
            return Coin.level(selectedItemPosition + 1, withDecimals)
        }
        else
            throw IllegalStateException("Cannot identify the view controlled by the coin selector")
    }
}