package com.hellow.text;

/**
 * Truncation / wrapping mode for a single line of text rendered into a
 * fixed-width container. Picked by the caller per text region.
 *
 * <table border="1">
 *   <caption>Mode behaviour summary</caption>
 *   <tr><th>Mode</th><th>Behaviour</th><th>Use case</th></tr>
 *   <tr><td>{@link #COLLAPSIBLE}</td>
 *       <td>Truncate to the first line that fits, append a '…' (U+2026)
 *           and a small "[+]" affordance the embedding mod can render
 *           as a clickable expand toggle.</td>
 *       <td>Long descriptions, lists, narrative strings.</td></tr>
 *   <tr><td>{@link #WRAP}</td>
 *       <td>Soft-wrap on word boundaries until the container is full;
 *           grow vertically as needed.</td>
 *       <td>Help text, install guides, body paragraphs.</td></tr>
 *   <tr><td>{@link #CLAMPED}</td>
 *       <td>Truncate to fit, append '…' if anything was cut. No
 *           expand affordance — the truncated form is the only form.</td>
 *       <td>URLs, IDs, table cells, anything that must fit one line.</td></tr>
 * </table>
 *
 * <p>(ponytail: design-doc calls these COLLAPSIBLE / WRAP / CLAMPED.
 * Kept the names verbatim so the design doc and code stay in lockstep.
 * If you rename here, update §3 of 最新更新.md too.)
 */
public enum StringMode {
    COLLAPSIBLE,
    WRAP,
    CLAMPED
}
