package org.thoughtcrime.securesms.components.menu

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.ViewUtil

/**
 * A bar that displays a set of action buttons. Intended as a replacement for ActionModes, this gives you a simple interface to add a bunch of actions, and
 * the bar itself will handle putting things in the overflow and whatnot.
 *
 * Overflow items are rendered in a [SignalContextMenu].
 */
class SignalBottomActionBar(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet) {

  val items: MutableList<ActionItem> = mutableListOf()

  init {
    orientation = HORIZONTAL
    setBackgroundResource(R.drawable.signal_bottom_action_bar_background)

    if (Build.VERSION.SDK_INT >= 21) {
      elevation = 20f
    }
  }

  fun setItems(items: List<ActionItem>) {
    this.items.clear()
    this.items.addAll(items)
    present(this.items)
  }

  private fun present(items: List<ActionItem>) {
    if (width == 0) {
      post { present(items) }
      return
    }

    val widthDp: Float = ViewUtil.pxToDp(width.toFloat())
    val minButtonWidthDp = 70
    val maxButtons: Int = (widthDp / minButtonWidthDp).toInt()
    val usableButtonCount = when {
      items.size <= maxButtons -> items.size
      else -> maxButtons - 1
    }

    val renderableItems: List<ActionItem> = items.subList(0, usableButtonCount)
    val overflowItems: List<ActionItem> = if (renderableItems.size < items.size) items.subList(usableButtonCount, items.size) else emptyList()

    removeAllViews()

    renderableItems.forEach { item ->
      val view: View = LayoutInflater.from(context).inflate(R.layout.signal_bottom_action_bar_item, this, false)
      addView(view)
      bindItem(view, item)
    }

    if (overflowItems.isNotEmpty()) {
      val view: View = LayoutInflater.from(context).inflate(R.layout.signal_bottom_action_bar_item, this, false)
      addView(view)
      bindItem(
        view,
        ActionItem(
          iconRes = R.drawable.ic_more_horiz_24,
          title = context.getString(R.string.SignalBottomActionBar_more),
          action = {
            SignalContextMenu.Builder(view, parent as ViewGroup)
              .preferredHorizontalPosition(SignalContextMenu.HorizontalPosition.END)
              .offsetY(ViewUtil.dpToPx(8))
              .show(overflowItems)
          }
        )
      )
    }
  }

  private fun bindItem(view: View, item: ActionItem) {
    val icon: ImageView = view.findViewById(R.id.signal_bottom_action_bar_item_icon)
    val title: TextView = view.findViewById(R.id.signal_bottom_action_bar_item_title)

    icon.setImageResource(item.iconRes)
    title.text = item.title
    view.setOnClickListener { item.action.run() }
  }
}