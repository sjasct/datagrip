/*
 * DataGrip -> Microsoft 365 table extractor
 */

// Change these if you prefer blank database NULLs or no column headings.
def nullText = 'NULL'
def includeColumnHeaders = true

def cellStyle = 'border:1px solid #b7b7b7;padding:4px 8px;vertical-align:top;text-align:left;white-space:pre-wrap;'
def headerStyle = cellStyle + 'font-weight:bold;'

// Treat every database value as text, including values that look like HTML.
def escapeHtml = { value ->
  def text = value == null ? '' : value.toString()
  def escaped = text.replace('&', '&amp;')
                    .replace('<', '&lt;')
                    .replace('>', '&gt;')
                    .replace('"', '&quot;')
                    .replace("'", '&#39;')
                    .replace('\r\n', '\n')
                    .replace('\r', '\n')
                    .replace('\n', '<br>')
  escaped.isEmpty() ? '&#160;' : escaped
}

def cellText = { row, column ->
  row.value(column) == null ? nullText : FORMATTER.format(row, column)
}

def printCell = { value, header ->
  def tag = header ? 'th' : 'td'
  def style = header ? headerStyle : cellStyle
  OUT.append("<${tag} style=\"${style}\">${escapeHtml(value)}</${tag}>")
}

OUT.append('''<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>DataGrip table</title></head><body>
<!--StartFragment-->
<table border="1" cellspacing="0" cellpadding="0" style="border-collapse:collapse;font-family:Segoe UI,Arial,sans-serif;font-size:11pt;">
''')

if (!TRANSPOSED) {
  if (includeColumnHeaders && !COLUMNS.isEmpty()) {
    OUT.append('<thead><tr>')
    COLUMNS.each { column -> printCell(column.name(), true) }
    OUT.append('</tr></thead>\n')
  }
  OUT.append('<tbody>\n')
  ROWS.each { row ->
    OUT.append('<tr>')
    COLUMNS.each { column -> printCell(cellText(row, column), false) }
    OUT.append('</tr>\n')
  }
}
else {
  // Transposing a one-pass row stream requires buffering the formatted values.
  def valuesByColumn = COLUMNS.collect { new ArrayList<String>() }
  ROWS.each { row ->
    COLUMNS.eachWithIndex { column, index ->
      valuesByColumn[index].add(cellText(row, column))
    }
  }
  OUT.append('<tbody>\n')
  COLUMNS.eachWithIndex { column, index ->
    OUT.append('<tr>')
    if (includeColumnHeaders) printCell(column.name(), true)
    valuesByColumn[index].each { value -> printCell(value, false) }
    OUT.append('</tr>\n')
  }
}

OUT.append('''</tbody>
</table>
<!--EndFragment-->
</body></html>
''')
