EcoNest - images and icons
==========================

Everything in this folder is loaded through com.econest.ui.Icons, which looks
in /images/icons/ first and then /images/. Nothing here is required: if a file
is missing, Icons returns null and the screen simply leaves the image out. So
you can drop artwork in later without touching any Java code.

Never hard-code a path to this folder. Always go through Icons:

    Icon nav  = Icons.icon("inventory", 18, Theme.TEXT_ON_DARK);  // SVG, tinted
    Icon logo = Icons.image("logo.png", 150, 44);                 // PNG, scaled


images/  - photos and the logo (PNG)
------------------------------------

  logo.png              about 256 px wide, transparent background.
                        Shown at the top of the sidebar. The sidebar falls
                        back to the "EcoNest" text when it is absent, and
                        shows the text under the logo when it is present.

  dashboard-plants.png  about 400 x 250, photo or illustration.
                        Sits on the right of the welcome card on the
                        Dashboard. Scaled to fit while keeping its aspect
                        ratio, so a larger file is fine - it just has to be
                        roughly landscape.


images/icons/  - line icons (SVG)
---------------------------------

24 x 24 viewBox, stroke-based, no fill. They are drawn in #2F6F55 but every
caller re-tints them through Icons.icon(name, size, colour), so the colour in
the file itself does not matter much - only that the shape is a clean stroke.

Sidebar, one per navigation item:

  dashboard.svg         Dashboard
  products.svg          Products
  clients.svg           Clients
  inventory.svg         Inventory
  agents.svg            Delivery Agents
  orders.svg            Orders
  assignment.svg        Delivery Assignment
  reports.svg           Reports
  notifications.svg     Notifications

Dashboard cards and decoration:

  alert.svg             Low stock items card
  clock.svg             Pending orders card
  revenue.svg           This month's revenue card
  leaf.svg              Welcome card
                        (the deliveries card reuses assignment.svg)

The icons committed here are simple placeholders so the app looks right out
of the box. Replace them with the real icon set whenever it is ready - keep
the same file names and the 24 x 24 stroke style and nothing else needs to
change.


Adding a new image
------------------

1. Drop the file in images/ (bitmap) or images/icons/ (SVG).
2. Add a line to this file saying what it is and what size it should be.
3. Load it with Icons.icon(...) or Icons.image(...) and check for null.
