# Group Attendance Tracker

A RuneLite plugin that tracks how long nearby players stay in your vicinity. Built for clan leaders, event organizers, and anyone who needs to log group attendance during in-game events.

## Features

### Player Tracking
- Tracks attendance time for all nearby players in real time (measured in game ticks, displayed as MM:SS)
- Filter which players to track:
  - **Clan Chat** members
  - **Friends Chat** members
  - **Guest Clan** members (for when you're guesting in another clan)
  - **All visible players**
- Option to include or exclude yourself from the attendance list

### Sidebar Panel
- Dedicated sidebar panel with a styled player list
- Each player shown with rank number, name, and duration
- Live player count
- **Start / Stop / Reset** controls
- **Copy to Clipboard** for easy sharing of attendance results

### In-Game Overlay
- On-screen overlay showing nearby tracked players
- Configurable max players displayed (1-50)
- Optional player count in header
- Movable, snappable, and drag-targetable
- Can be toggled on/off

### Persistence
- Attendance data is automatically saved and restored across world hops, relogs, and client restarts
- Auto-saves every ~30 seconds during active tracking
- Reset button clears both active and saved data

## Configuration

| Setting | Description | Default |
|---|---|---|
| Show Overlay | Show the on-screen attendance overlay | On |
| Tracking Enabled | Enable/disable attendance tracking | On |
| Track Yourself | Include your own character in tracking | On |
| Track Clan Chat | Track members of your clan | On |
| Track Friends Chat | Track friends chat members | On |
| Track Guest Clan | Track members of the clan you are guesting in | Off |
| Track All Players | Track all visible players | On |
| Max Players in Overlay | Max players shown in the overlay (1-50) | 25 |
| Show Player Count | Show player count in overlay header | On |
| Sort Alphabetically | Sort player names alphabetically | On |

## Usage

1. Install the plugin from the Plugin Hub.
2. Open the sidebar panel by clicking the Group Attendance icon in the RuneLite toolbar.
3. Log in and nearby players matching your filter settings will start being tracked automatically.
4. Use **Stop** to pause tracking and **Start** to resume.
5. Use **Copy to Clipboard** to grab the full attendance list for sharing.
6. Use **Reset** to clear all data and start a fresh session.

## License

BSD 2-Clause License. See [LICENSE](LICENSE) for details.
