package com.example.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GlassThickness
import com.example.data.NoteEntity
import com.example.data.TaskWithDetails
import com.example.ui.glass.GlassChip
import com.example.ui.glass.GlassEmptyState
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.LocalGlassTokens
import com.example.ui.tasks.GlassDateTimePickerDialog
import com.example.ui.theme.CuratedAccentPalettes
import com.example.ui.theme.parseColorHex
import com.example.util.S
import java.time.LocalDate

@Composable
fun NotesModeScreen(
    notes: List<NoteEntity>,
    tasks: List<TaskWithDetails>,
    onSaveNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onConvertChecklistToTasks: (List<String>, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = S.current
    val tokens = LocalGlassTokens.current

    var isGridView by remember { mutableStateOf(true) }
    var showArchived by remember { mutableStateOf(false) }
    var showDayNotesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes by remember(notes, showArchived, showDayNotesOnly, searchQuery) {
        derivedStateOf {
            notes.filter { n ->
                !n.isDeleted &&
                    (n.isArchived == showArchived) &&
                    (!showDayNotesOnly || n.isDayNote) &&
                    (searchQuery.isBlank() ||
                        n.title.contains(searchQuery, ignoreCase = true) ||
                        n.contentMarkdown.contains(searchQuery, ignoreCase = true) ||
                        n.tagsCsv.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.modeNotes,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip(
                    label = if (isGridView) "Grid" else "List",
                    leadingIcon = {
                        Icon(
                            if (isGridView) Icons.Filled.GridView else Icons.Filled.ViewAgenda,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    onClick = { isGridView = !isGridView }
                )
                GlassChip(
                    label = if (s.isVi) "+ Ghi chú" else "+ Note",
                    selected = true,
                    onClick = {
                        editingNote = NoteEntity(
                            title = "",
                            contentMarkdown = "",
                            attachedDateEpochDay = LocalDate.now().toEpochDay()
                        )
                    },
                    modifier = Modifier.testTag("add_note_btn")
                )
            }
        }

        // Search & Filter Bar
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            thickness = GlassThickness.THIN,
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(s.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassChip(
                label = if (s.isVi) "Đang hoạt động" else "Active Notes",
                selected = !showArchived && !showDayNotesOnly,
                onClick = { showArchived = false; showDayNotesOnly = false }
            )
            GlassChip(
                label = "✦ ${s.dayNoteTitle}",
                selected = showDayNotesOnly,
                onClick = { showDayNotesOnly = !showDayNotesOnly }
            )
            GlassChip(
                label = if (s.isVi) "Lưu trữ" else "Archived",
                selected = showArchived,
                leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(15.dp)) },
                onClick = { showArchived = !showArchived }
            )
        }

        if (filteredNotes.isEmpty()) {
            GlassEmptyState(
                message = s.emptyNotes,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isGridView) 2 else 1),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    val noteAccent = parseColorHex(note.colorHex, tokens.accentColor)
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = if (note.isPinned) GlassThickness.THICK else GlassThickness.REGULAR,
                        cornerRadius = 20.dp,
                        accentTint = noteAccent,
                        onClick = { editingNote = note },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title.ifBlank { if (note.isDayNote) s.dayNoteTitle else "Untitled Note" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (note.isPinned) {
                                    Icon(
                                        Icons.Filled.PushPin,
                                        contentDescription = "Pinned",
                                        tint = noteAccent,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            MarkdownLitePreview(
                                markdown = note.contentMarkdown,
                                maxLines = 6
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                note.attachedDateEpochDay?.let { epoch ->
                                    val d = LocalDate.ofEpochDay(epoch)
                                    Text(
                                        text = "📅 ${d.dayOfMonth}/${d.monthValue}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = noteAccent
                                    )
                                }
                                if (note.tagsCsv.isNotBlank()) {
                                    Text(
                                        text = note.tagsCsv.split(",").firstOrNull()?.let { "#${it.trim()}" }.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = tokens.secondaryAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingNote?.let { target ->
        NoteMarkdownEditorDialog(
            initialNote = target,
            tasks = tasks,
            onDismiss = { editingNote = null },
            onSave = { updated ->
                onSaveNote(updated)
                editingNote = null
            },
            onDelete = { id ->
                onDeleteNote(id)
                editingNote = null
            },
            onConvertChecklistToTasks = onConvertChecklistToTasks
        )
    }
}

/**
 * Markdown-lite Editor with Toolbar (H1, H2, Bold, Italic, Strike, Bullet, Numbered, Checklist, Quote, Code, Link),
 * Attach to Date, Link to Task, and 1-Tap "Convert Checklist Lines to Tasks".
 */
@Composable
fun NoteMarkdownEditorDialog(
    initialNote: NoteEntity,
    tasks: List<TaskWithDetails>,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onConvertChecklistToTasks: (List<String>, Long?) -> Unit
) {
    val s = S.current
    val tokens = LocalGlassTokens.current

    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.contentMarkdown) }
    var attachedEpochDay by remember { mutableStateOf(initialNote.attachedDateEpochDay) }
    var linkedTaskId by remember { mutableStateOf(initialNote.linkedTaskId) }
    var colorHex by remember { mutableStateOf(initialNote.colorHex) }
    var tagsCsv by remember { mutableStateOf(initialNote.tagsCsv) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }
    var isArchived by remember { mutableStateOf(initialNote.isArchived) }
    var showDatePicker by remember { mutableStateOf(false) }
    var convertedBannerCount by remember { mutableStateOf(0) }

    val checklistItems = remember(content) {
        content.lines()
            .map { it.trim() }
            .filter { it.startsWith("- [ ] ") || it.startsWith("* [ ] ") }
            .map { it.removePrefix("- [ ] ").removePrefix("* [ ] ").trim() }
            .filter { it.isNotBlank() }
    }

    fun appendSnippet(snippet: String) {
        content = if (content.isBlank() || content.endsWith("\n")) {
            content + snippet
        } else {
            "$content\n$snippet"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            thickness = GlassThickness.THICK,
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (s.isVi) "Trình soạn Ghi chú Markdown" else "Markdown Note Editor",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Pin",
                                tint = if (isPinned) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isArchived = !isArchived }) {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = "Archive",
                                tint = if (isArchived) tokens.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (initialNote.id != 0L) {
                            IconButton(onClick = { onDelete(initialNote.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = s.delete, tint = Color(0xFFF43F5E))
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = s.cancel)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(if (s.isVi) "Tiêu đề ghi chú…" else "Note title…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Markdown-Lite Formatting Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "H1" to "# ",
                        "H2" to "## ",
                        "B" to "**bold** ",
                        "I" to "*italic* ",
                        "S" to "~~strike~~ ",
                        "• List" to "- ",
                        "1. Num" to "1. ",
                        "☑ Check" to "- [ ] ",
                        "❝ Quote" to "> ",
                        "</> Code" to "`code` ",
                        "🔗 Link" to "[title](https://)"
                    ).forEach { (btnLabel, syntax) ->
                        GlassChip(
                            label = btnLabel,
                            onClick = { appendSnippet(syntax) }
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(if (s.isVi) "Nội dung Markdown…" else "Write with Markdown…") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                // Convert Checklist Lines to Tasks Action
                if (checklistItems.isNotEmpty()) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.REGULAR,
                        cornerRadius = 16.dp,
                        accentTint = tokens.accentColor,
                        onClick = {
                            onConvertChecklistToTasks(checklistItems, attachedEpochDay ?: LocalDate.now().toEpochDay())
                            convertedBannerCount = checklistItems.size
                            content = content.replace("- [ ] ", "- [x] ").replace("* [ ] ", "* [x] ")
                        },
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.TaskAlt, contentDescription = null, tint = tokens.accentColor)
                                Text(
                                    text = if (s.isVi) "Chuyển ${checklistItems.size} mục checklist thành Công việc"
                                    else "Convert ${checklistItems.size} checklist lines to Tasks",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text("→", color = tokens.accentColor, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (convertedBannerCount > 0) {
                    Text(
                        text = if (s.isVi) "✓ Đã tạo $convertedBannerCount công việc mới!"
                        else "✓ Created $convertedBannerCount tasks in your planner!",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF34D399)
                    )
                }

                // Live Formatted Preview
                if (content.isNotBlank()) {
                    Text(if (s.isVi) "Xem trước:" else "Live Preview:", style = MaterialTheme.typography.labelMedium)
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = GlassThickness.THIN,
                        cornerRadius = 14.dp,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        MarkdownLitePreview(markdown = content, maxLines = 12)
                    }
                }

                // Attach to Calendar Date & Link to Task
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateLabel = attachedEpochDay?.let { LocalDate.ofEpochDay(it).toString() }
                        ?: (if (s.isVi) "Gắn vào ngày" else "Attach to Date")
                    GlassChip(
                        label = "📅 $dateLabel",
                        selected = attachedEpochDay != null,
                        onClick = { showDatePicker = true }
                    )

                    tasks.take(5).forEach { t ->
                        GlassChip(
                            label = "🔗 ${t.task.title.take(18)}",
                            selected = linkedTaskId == t.task.id,
                            onClick = {
                                linkedTaskId = if (linkedTaskId == t.task.id) null else t.task.id
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = tagsCsv,
                    onValueChange = { tagsCsv = it },
                    label = { Text(if (s.isVi) "Thẻ (cách nhau dấu phẩy)" else "Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // Color Swatches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CuratedAccentPalettes.forEach { pal ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(pal.primary)
                                .border(
                                    width = if (colorHex.equals(pal.hex, ignoreCase = true)) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = pal.hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(s.cancel) }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassChip(
                        label = s.save,
                        selected = true,
                        onClick = {
                            onSave(
                                initialNote.copy(
                                    title = title.trim().ifBlank { content.lines().firstOrNull()?.take(32) ?: "Note" },
                                    contentMarkdown = content,
                                    attachedDateEpochDay = attachedEpochDay,
                                    linkedTaskId = linkedTaskId,
                                    colorHex = colorHex,
                                    tagsCsv = tagsCsv,
                                    isPinned = isPinned,
                                    isArchived = isArchived
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        GlassDateTimePickerDialog(
            initialEpochDay = attachedEpochDay,
            initialTimeMinute = null,
            onDismiss = { showDatePicker = false },
            onConfirm = { d, _, _ ->
                attachedEpochDay = d
                showDatePicker = false
            }
        )
    }
}

@Composable
fun MarkdownLitePreview(
    markdown: String,
    maxLines: Int = 10,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val annotated = remember(markdown, tokens.accentColor) {
        buildAnnotatedString {
            val lines = markdown.lines()
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trimEnd()
                when {
                    line.startsWith("# ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = tokens.accentColor)) {
                            append(line.removePrefix("# "))
                        }
                    }
                    line.startsWith("## ") -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = tokens.secondaryAccent)) {
                            append(line.removePrefix("## "))
                        }
                    }
                    line.startsWith("- [x] ") || line.startsWith("* [x] ") -> {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray)) {
                            append("☑ " + line.substring(6))
                        }
                    }
                    line.startsWith("- [ ] ") || line.startsWith("* [ ] ") -> {
                        append("☐ " + line.substring(6))
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        append("• " + line.substring(2))
                    }
                    line.startsWith("> ") -> {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = tokens.secondaryAccent)) {
                            append("❝ " + line.removePrefix("> "))
                        }
                    }
                    else -> {
                        // Inline bold/italic/code/strike parsing
                        val clean = line
                            .replace("**", "")
                            .replace("~~", "")
                            .replace("`", "")
                        if (line.contains("`")) {
                            withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = tokens.accentColor)) {
                                append(clean)
                            }
                        } else if (line.contains("**")) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(clean)
                            }
                        } else if (line.contains("~~")) {
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                append(clean)
                            }
                        } else {
                            append(line)
                        }
                    }
                }
                if (index < lines.lastIndex) append("\n")
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
