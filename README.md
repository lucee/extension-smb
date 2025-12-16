# Lucee Extension SMB Resource Provider

[![Java CI](https://github.com/lucee/extension-smb/actions/workflows/main.yml/badge.svg)](https://github.com/lucee/extension-smb/actions/workflows/main.yml)

This extension provides SMB (Server Message Block) / CIFS (Common Internet File System) virtual file system support for Lucee, allowing you to read, write, and manipulate files on Windows network shares and Samba servers using standard CFML file functions.

## Documentation

- [Lucee Virtual File Systems](https://docs.lucee.org/guides/lucee-5/virtual-file-systems.html)
- [Working with Files and Directories](https://docs.lucee.org/guides/Various/working-with-files.html)

## Usage

Access SMB shares using the `smb://` scheme with standard CFML file functions:

```cfml
// Basic usage with credentials in URL
smbPath = "smb://username:password@server/share/path/";

// Check if directory exists
if ( directoryExists( smbPath ) ) {
    // List files
    files = directoryList( smbPath );

    // Read a file
    content = fileRead( smbPath & "document.txt" );

    // Write a file
    fileWrite( smbPath & "output.txt", "Hello SMB!" );
}

// With domain authentication
smbPath = "smb://DOMAIN;username:password@server/share/";
```

### URL Format

```text
smb://[domain;]username:password@host[:port]/share/path/
```

- **domain** - Optional Windows domain
- **username** - SMB username
- **password** - SMB password
- **host** - Server hostname or IP
- **port** - Optional port (default: 445)
- **share** - Share name
- **path** - Path within the share

### Supported Operations

- `directoryCreate()` / `directoryDelete()` / `directoryExists()` / `directoryList()`
- `fileRead()` / `fileWrite()` / `fileDelete()` / `fileExists()`
- `fileCopy()` / `fileMove()`
- `getFileInfo()` / `fileSetLastModified()`

## Configuration

The extension works out of the box with sensible defaults. To customise the SMB settings, add arguments to your `.CFConfig.json`:

```json
{
  "resourceProviders": [
    {
      "scheme": "smb",
      "class": "org.lucee.extension.resource.smb.SMBResourceProvider",
      "arguments": {
        "minVersion": "SMB202",
        "signingPreferred": "true"
      }
    }
  ]
}
```

### Available Options

| Option | Default | Description |
|--------|---------|-------------|
| `minVersion` | `SMB1` | Minimum SMB protocol version: `SMB1`, `SMB202`, `SMB210`, `SMB300`, `SMB302`, `SMB311` |
| `maxVersion` | `SMB311` | Maximum SMB protocol version |
| `soTimeout` | `35000` | Socket timeout in milliseconds |
| `connTimeout` | `35000` | Connection timeout in milliseconds |
| `responseTimeout` | `30000` | SMB response timeout in milliseconds |
| `signingPreferred` | `false` | Prefer SMB signing when available |
| `signingEnforced` | `false` | Require SMB signing (fails if server doesn't support it) |
| `encryptionEnabled` | `false` | Enable SMB3 encryption |
| `dfsDisabled` | `true` | Disable DFS (Distributed File System) lookups |
| `resolveOrder` | `DNS` | Name resolution order (e.g., `DNS`, `LMHOSTS,DNS,WINS,BCAST`) |

### Security Recommendations

For modern environments, consider:

```json
{
  "arguments": {
    "minVersion": "SMB300",
    "signingEnforced": "true",
    "encryptionEnabled": "true"
  }
}
```

This enforces SMB3+ with signing and encryption, which is the most secure configuration.

## Requirements

- Lucee 7.x or later
- Java 17 or later

## Technical Details

This extension uses [jcifs-codelibs 3.0.0](https://github.com/codelibs/jcifs), a modern fork of the JCIFS library with SMB2/SMB3 support.

## Issues

[Lucee JIRA - SMB Issues](https://luceeserver.atlassian.net/issues/?jql=labels%20%3D%20smb)
