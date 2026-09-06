package universe.constellation.orion.viewer.outline

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import universe.constellation.orion.viewer.DocPlace
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.document.OutlineItem

/**
 * Navigation history as a timeline: back entries (oldest first), the current place,
 * then forward entries. Row titles come from the nearest outline chapter.
 */
class HistoryAdapter(
    private val back: List<DocPlace>,
    private val current: DocPlace?,
    private val forward: List<DocPlace>,
    private val outline: Array<OutlineItem>?,
    private val onNavigate: (Entry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    enum class Kind { BACK, CURRENT, FORWARD }

    /** [index] is the position inside the corresponding history stack list. */
    class Entry(val kind: Kind, val index: Int, val place: DocPlace)

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val direction: AppCompatImageView = view.findViewById(R.id.direction)
        val title: TextView = view.findViewById(R.id.title)
        val place: TextView = view.findViewById(R.id.place)
    }

    private val entries: List<Entry> = buildList {
        back.forEachIndexed { i, place -> add(Entry(Kind.BACK, i, place)) }
        current?.let { add(Entry(Kind.CURRENT, -1, it)) }
        forward.forEachIndexed { i, place -> add(Entry(Kind.FORWARD, i, place)) }
    }

    val currentPosition: Int = entries.indexOfFirst { it.kind == Kind.CURRENT }

    override fun getItemCount(): Int = entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_entry, parent, false)
        val holder = Holder(view)
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            val entry = entries[position]
            if (entry.kind != Kind.CURRENT) onNavigate(entry)
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context

        holder.direction.setImageResource(
            when (entry.kind) {
                Kind.BACK -> R.drawable.outline_history_back
                Kind.CURRENT -> R.drawable.outline_history_current
                Kind.FORWARD -> R.drawable.outline_history_forward
            }
        )
        androidx.core.widget.ImageViewCompat.setImageTintList(
            holder.direction,
            if (entry.kind == Kind.CURRENT) null else holder.place.textColors
        )

        val chapter = outline?.lastOrNull { it.page in 0..entry.place.page }
        val pageText = context.getString(R.string.outline_history_page, entry.place.page + 1)
        holder.title.text = chapter?.title ?: pageText
        holder.place.text = pageText

        val isCurrent = entry.kind == Kind.CURRENT
        holder.itemView.setBackgroundColor(if (isCurrent) context.listHighlightColor else 0)
        holder.title.setTypeface(null, if (isCurrent) Typeface.BOLD else Typeface.NORMAL)
    }
}
