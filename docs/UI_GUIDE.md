# EcoNest UI Guide

How to build your module's screen on the shared UI foundation.

Everything you need lives in two packages:

| Package | What's in it |
| --- | --- |
| `com.econest.ui` | `Theme`, `MainFrame`, `BaseModulePanel`, `PlaceholderPanel`, `DashboardPanel`, `DashboardCard` / `DashboardCards` / `StatCard` |
| `com.econest.ui.components` | `Buttons`, `TableStyler`, `FormFields`, `Dialogs`, `InputValidator` |

The worked example is **`com.econest.inventory.ui.InventoryPanel`** — read it next
to this guide. It is a full screen in about 200 lines.

---

## Build a screen in 5 steps

### 1. Extend `BaseModulePanel`

The base class already gives you the approved layout: title + search box on
top, table in the centre, form on the right, `Add / Update / Delete / Clear`
along the bottom.

```java
package com.econest.client.ui;

import com.econest.ui.BaseModulePanel;

public class ClientPanel extends BaseModulePanel {

    private static final String[] COLUMNS = {"ID", "Name", "Type", "Phone", "Status"};

    public ClientPanel() {
        super("Clients", COLUMNS);

        TableStyler.colourStatusColumn(tblData, 4);  // optional: colour "Status"

        initPanel();   // MUST be the last line of the constructor
    }
    ...
}
```

`initPanel()` builds your form and loads the table. It has to come **last**,
because your own fields don't exist yet while `super(...)` is running.

### 2. Fill in the form

Declare your fields, then add them in `buildForm`. `FormFields.row(...)`
gives each field a caption and a hidden inline error label.

```java
private JTextField txtName;
private JComboBox<String> cmbType;

@Override
protected void buildForm(JPanel form) {
    txtName = FormFields.textField();
    cmbType = FormFields.comboBox(new String[]{"Household", "Office"});

    form.add(FormFields.heading("Client details"));
    form.add(FormFields.row("Name", txtName));
    form.add(FormFields.row("Type", cmbType));
}
```

### 3. Connect your own DAO and service

The panel talks to your **service**, the service talks to your **DAO**. Never
put SQL in a panel.

```java
private final ClientService service = new ClientService(new ClientDaoImpl());

@Override
protected void loadTableData() {
    List<Client> clients = service.findAll();
    tableModel.setRowCount(0);
    for (Client c : clients) {
        tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getType(),
                                       c.getPhone(), c.getStatus()});
    }
    setStatus(clients.size() + " clients loaded.");
}
```

Then override the hooks you need. Each one already has a harmless default, so
you can do them one at a time:

| Hook | When it runs | Notes |
| --- | --- | --- |
| `buildForm(JPanel form)` | once, from `initPanel()` | **required** |
| `loadTableData()` | once, and whenever you call it | **required** |
| `onAdd()` | Add pressed | validate, save, reload, `onClear()` |
| `onUpdate()` | Update pressed | acts on the selected row |
| `onDelete()` | Delete pressed **and confirmed** | the dialog is already done for you |
| `onClear()` | Clear pressed | empty your fields, then call `super.onClear()` |
| `onSearch(String query)` | every keystroke in the search box | empty string means "show everything" |

Useful inherited members: `tblData`, `tableModel`, `pnlForm`, `txtSearch`,
`btnAdd` / `btnUpdate` / `btnDelete` / `btnClear`, `setRows(Object[][])`,
`selectedModelRow()`, `selectedValue(int column)` and `setStatus(String)`.

> **Careful with row indexes.** `selectedModelRow()` is the row in the *table*.
> If your `onSearch` shows a filtered subset, look the record up by its ID
> instead — `InventoryPanel.selectedSourceRow()` shows the pattern.

### 4. Register your panel with one line in `MainFrame`

Open `com.econest.ui.MainFrame`, find `registerPages()` and change **only your
own line**. Each module has one line with a blank line around it, so two
people editing different modules never touch the same diff hunk.

```java
// Clients module
addPage(PAGE_CLIENTS, new ClientPanel());     // was: new PlaceholderPanel(...)
```

Add your import at the top of the file next to the other module imports.
Don't reorder, reformat or re-indent anyone else's line.

### 4b. Add your card to the Dashboard

The Dashboard builds itself from a list, so you never edit `DashboardPanel`:

1. Open `com.econest.ui.DashboardCards` and find **your** module's line — it
   holds a `StatCard.placeholder(...)` showing `0`.
2. Replace it with a real card, keeping it to one line:
   `new StatCard("Pending orders", "orders", Theme.TINT_PENDING, Theme.STATUS_PENDING, () -> String.valueOf(service.countByStatus(PENDING)))`
3. The last argument is a `StatCard.ValueSource` — it runs on a **background
   thread**, so put your service call there and let it throw if it fails.
4. A failure shows `0` and logs one line; the Dashboard never breaks and never
   pops up a dialog.
5. Need more than one big number? Implement `DashboardCard` yourself
   (`getTitle` / `getView` / `refresh`) and read its threading note first.

### 5. Use `Theme` and the helpers — never hard-code

Any colour, font, padding or gap comes from `com.econest.ui.Theme`. If you
need a shade that isn't there, **add it to `Theme`** so everyone can use it.

```java
// NO
label.setForeground(new Color(0x2E, 0x7D, 0x32));
panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

// YES
label.setForeground(Theme.STATUS_SUCCESS);
panel.setBorder(BorderFactory.createEmptyBorder(
        Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
```

---

## The helpers at a glance

**`Buttons`** — `Buttons.primary("Save")` (filled green),
`Buttons.secondary("Refresh")` (outlined), `Buttons.danger("Remove")`
(outlined red), `Buttons.text("Clear")` (no chrome), plus
`Buttons.link("View all")` and `Buttons.icon(icon, "tooltip")`. Add extras to
`pnlActions`, which is a 2-column grid so they wrap inside the form card.
Always give an icon-only button a tooltip — it has no label to explain it.

**`Card`** — the white rounded surface everything sits on:
`new Card(new BorderLayout()).withPadding()`, or
`Card.titled("Low stock", content, optionalHeaderAction)`. It paints its own
border and soft shadow, so don't add one.

**`EmptyState`** — `EmptyState.wrap(scrollPane, tbl, "No items found", "hint")`
returns a container that shows your table while it has rows and a friendly
empty panel when it doesn't. `BaseModulePanel` already does this for
`tblData`; change the wording with `setEmptyState(message, hint)`.

**`TableStyler`** — `style(tbl)` is already applied to `tblData`. Add
`TableStyler.colourStatusColumn(tblData, index)` to colour a status column:
green for *delivered / completed / in stock / active*, amber for *pending /
processing / in transit / assigned*, red for *failed / cancelled / low stock /
out of stock*, blue for *new / draft / scheduled*. Anything else stays the
normal text colour, so a new status word never breaks the table.
`colourStatusColumn` renders the column as a rounded pill badge, not plain
text. `rightAlign(tbl, 2, 3)` right-aligns numeric columns, which is how
numbers should read. `fitColumns(tbl)` sizes columns to their content —
`setRows(...)` already calls it, so you only need it if you fill the model
some other way. Columns squeeze to fit and only scroll sideways on a large
overflow, so the status pill stays visible.

**`FormFields`** — `textField()`, `comboBox(items)`, `label(text)`,
`heading(text)`, `row(caption, input)`.

**`Icons`** — `Icons.icon("inventory", 18, Theme.TEXT_ON_DARK)` for an SVG
from `resources/images/icons` (the colour argument re-tints it), and
`Icons.image("logo.png", 150, 52)` for a scaled bitmap from
`resources/images`. Both return `null` when the file isn't there, so always
null-check and skip the image rather than assuming it loaded. File names and
sizes are listed in `src/main/resources/images/README.txt`.

**`Dialogs`** — `info`, `error`, `warn`, `confirm`, `confirmDelete`,
`errorAt(parent, message, field)`. The base class already calls
`confirmDelete` before `onDelete()`, so don't ask twice.

**`InputValidator`** — shows the problem *next to the field* (red border +
small red label) instead of in a pop-up:

```java
private final InputValidator validator = new InputValidator();

@Override
protected void onAdd() {
    validator.reset();
    String name  = validator.required(txtName, "Name");
    Integer qty  = validator.integer(txtQuantity, "Quantity");
    Double price = validator.number(txtPrice, "Price");
    String mail  = validator.email(txtEmail, "E-mail");
    validator.requiredSelection(cmbType, "Type");

    if (!validator.isValid()) {
        setStatus("Please fix the highlighted fields.");
        return;
    }
    // ... save
}
```

Each check returns the parsed value, or `null` when it failed. Clear the marks
in `onClear()` with `validator.clearAll(txtName, txtQuantity, ...)`.

**Status bar** — `setStatus("Saved client #12.")` from anywhere inside a
`BaseModulePanel`. Keep it to one short line; use `Dialogs` for anything the
user must acknowledge.

---

## Naming convention

Prefix every Swing component variable by its type:

| Prefix | Type | Example |
| --- | --- | --- |
| `txt` | `JTextField`, `JTextArea`, `JPasswordField` | `txtClientName` |
| `cmb` | `JComboBox` | `cmbClientType` |
| `btn` | `JButton` | `btnAssignAgent` |
| `tbl` | `JTable` | `tblOrders` |
| `lbl` | `JLabel` | `lblTotalAmount` |
| `dtp` | date picker / date field | `dtpDeliveryDate` |
| `pnl` | `JPanel` | `pnlForm` |
| `scr` | `JScrollPane` | `scrTable` |
| `chk` | `JCheckBox` | `chkActive` |

Other conventions:

- Classes: `ClientPanel`, `ClientService`, `ClientDao` — one module, one package.
- Table column headers in `static final String[] COLUMNS`.
- Panel talks to service; service talks to DAO. No SQL in a panel.

---

## Rules of the road

1. Only touch your own module package, plus your single line in
   `MainFrame.registerPages()` and your single line in
   `DashboardCards.register()`.
2. No `new Color(...)` or `new Font(...)` outside `Theme`.
3. Delete always goes through `Dialogs.confirmDelete` — the base class does
   this for you.
4. Keep Swing work on the Event Dispatch Thread. If a DAO call is slow, use a
   `SwingWorker` and call `setStatus(...)` when it finishes.
5. Compile before you push.
