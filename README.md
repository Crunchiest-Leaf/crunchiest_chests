# Crunchiest Chests

![Crunchiest Chests](https://img.shields.io/badge/version-1.0.0-brightgreen) ![License](https://img.shields.io/badge/license-MIT-blue)

## Description

**Crunchiest Chests** is a plugin for Minecraft servers that provides an alternative to the traditional treasure chest management system with a powerful SQLite backend. This plugin allows players to create and manage, treasure chests in a more streamlined way.

## Features

- **SQLite Backend**: Persistent storage of chest data using SQLite, ensuring data integrity and easy management.
- **Custom Commands**: Intuitive commands for creating, deleting, and updating chests.
- **Event Listeners**: Real-time interaction with chests through event listeners for various inventory actions.
- **Legacy Color Support**: Converts hex color codes to Minecraft legacy colors for customized chest names.

## Installation

1. Download the latest version of the plugin from the [Releases](https://github.com/Crunchiest-Leaf/crunchiest_chests/releases) section.
2. Place the downloaded JAR file into the `plugins` folder of your Minecraft server.
3. Restart the server to generate the plugin files.
4. Customize the plugin configuration as needed.

## Usage

### Commands

- `/make-chest`: Creates a new chest.
- `/delete-chest`: Deletes an existing chest.

### Example

1. To create a new chest, type `/make-chest #f0f0f0My Custom Chest` in the game. (custom names support hex colour codes)
2. To delete a chest, use the command `/delete-chest`.
3. Breaking a chest also removes it from the database!

## Configuration

The plugin creates a configuration file and a SQLite database upon startup. The database file will be located in the plugin's folder as `chests.db`. You can modify settings as needed.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Make your changes and commit them (`git commit -m 'Add your feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

**Crunchiest_Leaf**

- GitHub: [Crunchiest-Leaf](https://github.com/Crunchiest-Leaf)
- Description: A TChest Alternative, w/ SQLite Backend

## Acknowledgements

Special thanks to the Bukkit community for their support and resources.