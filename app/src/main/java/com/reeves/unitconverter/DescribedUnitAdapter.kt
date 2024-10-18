package com.reeves.unitconverter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class DescribedUnitAdapter(
    context: Context,
    resource: Int,
    private val describedUnits: List<DescribedUnit>,
) : ArrayAdapter<DescribedUnit>(
    context, resource, describedUnits
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        val nameTv = view.findViewById<TextView>(R.id.name_tv)
        val descriptionTv = view.findViewById<TextView>(R.id.description_tv)
        nameTv.text = describedUnits[position].name
        descriptionTv.text = describedUnits[position].description?.surroundWithParens()
        return view
    }

    private fun String.surroundWithParens(): String = "($this)"
}

data class DescribedUnit(val name: String, val description: String?) {
    override fun toString(): String = name
}
