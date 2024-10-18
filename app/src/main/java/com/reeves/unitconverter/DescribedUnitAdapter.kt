package com.reeves.unitconverter

import android.content.Context
import android.content.res.Resources
import android.database.DataSetObservable
import android.database.DataSetObserver
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ListAdapter
import android.widget.TextView
import android.widget.ThemedSpinnerAdapter

class DescribedUnitAdapter(
    private val context: Context,
    private val originalValues: List<DescribedUnit>,
) : ListAdapter, Filterable, ThemedSpinnerAdapter {

    private val filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            if (constraint.isNullOrEmpty()) {
                val list = originalValues.toList()
                results.values = list
                results.count = list.size
            } else {
                val prefixString = constraint.toString().lowercase()
                val newValues = mutableMapOf<DescribedUnit, Boolean>()
                for (describedUnit in originalValues) {
                    filterByAlts(describedUnit, prefixString, newValues)
                }
                // TODO make priority considered in sorting search results
                results.values = newValues.keys.toList()
                results.count = newValues.size
            }
            return results
        }

        private fun filterByAlts(
            describedUnit: DescribedUnit,
            searchString: String,
            newValues: MutableMap<DescribedUnit, Boolean>,
        ) {
            for (alt in describedUnit.alts) {
                if (searchString.length > alt.length) continue
                val lowerAlt = alt.lowercase()
                val startsWith = lowerAlt.startsWith(searchString)
                val contains = lowerAlt.contains(searchString)
                if (startsWith || (searchString.length > 1 && contains)
                ) {
                    newValues[describedUnit] = startsWith
                    return
                }
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            mutableValues.clear()
            if (results == null || results.count < 0) {
                dataSetObservable.notifyInvalidated()
            } else {
                mutableValues += results.values as List<DescribedUnit>
                dataSetObservable.notifyChanged()
            }
        }
    }

    private val mutableValues = originalValues.toMutableList()
    private val inflater = LayoutInflater.from(context)
    private var dropdownInflater: LayoutInflater? = null
    private val dataSetObservable = DataSetObservable()

    private fun createViewFromResource(
        inflater: LayoutInflater,
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: inflater.inflate(R.layout.list_item, parent, false)
        val nameTv = view.findViewById<TextView>(R.id.name_tv)
        val descriptionTv = view.findViewById<TextView>(R.id.description_tv)
        nameTv.text = mutableValues[position].name
        val alts = mutableValues[position].visibleAlts
        descriptionTv.text = if (alts.isEmpty()) "" else {
            val limit = 36 - nameTv.text.length
            var sum = 0
            alts.takeWhile { sum += it.length + 1; sum <= limit }
                .joinToString(separator = ", ", prefix = "(", postfix = ")")
        }
        return view
    }

    override fun registerDataSetObserver(observer: DataSetObserver?) =
        dataSetObservable.registerObserver(observer)

    override fun unregisterDataSetObserver(observer: DataSetObserver?) =
        dataSetObservable.unregisterObserver(observer)

    override fun getCount(): Int = mutableValues.size

    override fun getItem(position: Int): Any = mutableValues[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        createViewFromResource(inflater, position, convertView, parent)

    override fun getItemViewType(position: Int): Int = 0

    override fun getViewTypeCount(): Int = 1

    override fun isEmpty(): Boolean = count == 0

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        createViewFromResource(dropdownInflater ?: inflater, position, convertView, parent)

    override fun setDropDownViewTheme(theme: Resources.Theme?) {
        dropdownInflater = when (theme) {
            null -> null
            inflater.context.theme -> inflater
            else -> LayoutInflater.from(ContextThemeWrapper(context, theme))
        }
    }

    override fun getDropDownViewTheme(): Resources.Theme? {
        return dropdownInflater?.context?.theme
    }

    override fun areAllItemsEnabled(): Boolean = true

    override fun isEnabled(position: Int): Boolean = true

    override fun getFilter(): Filter = filter
}