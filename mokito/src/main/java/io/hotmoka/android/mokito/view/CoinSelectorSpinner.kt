package io.hotmoka.android.mokito.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ArrayAdapter
import io.hotmoka.android.mokito.R

class CoinSelectorSpinner constructor(
    context: Context, attrs: AttributeSet
) : androidx.appcompat.widget.AppCompatSpinner(context, attrs) {

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
}