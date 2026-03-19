# Whitelist Bypass

Tunnels internet traffic through VK call DataChannels to bypass government whitelist censorship.

## How it works

VK calls use WebRTC with an SFU (Selective Forwarding Unit). The SFU forwards all SCTP data channels between participants without inspecting them. This tool creates a custom DataChannel (id:2) alongside VK's built-in animoji channel (id:1) and uses it as a bidirectional data pipe.

```
Joiner (censored)                         Creator (free internet)

All apps
  |
VpnService (captures all traffic)
  |
tun2socks (IP -> TCP)
  |
SOCKS5 proxy (Go, :1080)
  |
WebSocket (:9000)
  |
WebView (VK call)                         Browser (VK call)
  |                                         |
DataChannel id:2  <--- VK SFU --->  DataChannel id:2
                                            |
                                        WebSocket (:9000)
                                            |
                                        Go relay
                                            |
                                        Internet
```

Traffic goes through VK's TURN servers (155.212.x.x:19302) which are whitelisted. To the network firewall it looks like a normal VK call.

## Components

- `hook.js` - Injected into VK call page on both sides. Hooks RTCPeerConnection, creates tunnel DataChannel, bridges to local WebSocket.
- `relay/` - Go binary and gomobile library. SOCKS5 proxy + WebSocket server + tun2socks.
- `app/` - Android app. WebView + VpnService + Go relay (.aar).

## Setup

### Creator side (free internet, PC)

1. Build the relay:
```
cd relay
go build -o relay .
```

2. Start it:
```
./relay --mode creator
```

3. Open Chrome, go to Sources -> Snippets, create snippet with contents of `hook.js`, run it.

4. Create a VK call, copy the join link, send it to the joiner.

### Joiner side (censored, Android)

1. Build the Go library:
```
./build-go.sh
```

2. Build the APK:
```
./build-app.sh
```

3. Install `whitelist-bypass.apk` on the phone.

4. Set the VK call link in `MainActivity.kt` (`VK_CALL_LINK` constant) or paste it in the app.

5. The app joins the call, establishes the tunnel, starts VPN. All device traffic flows through the VK call.

### Joiner side (censored, PC)

1. Start the relay:
```
./relay --mode joiner
```

2. Open Chrome, go to Sources -> Snippets, create snippet with contents of `hook.js`, run it.

3. Navigate to the VK call link.

4. Wait for `=== CALL CONNECTED ===` and `WebSocket connected to Go relay`.

5. Set system proxy to `socks5://127.0.0.1:1080`.

## Build requirements

- Go 1.21+
- gomobile (`go install golang.org/x/mobile/cmd/gomobile@latest`)
- gobind (`go install golang.org/x/mobile/cmd/gobind@latest`)
- Android SDK + NDK 27
- Java 11+
