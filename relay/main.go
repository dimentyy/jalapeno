package main

import (
	"flag"
	"fmt"
	"log"
	"os"

	"whitelist-bypass/relay/mobile"
)

type stdLogger struct{}

func (s stdLogger) OnLog(msg string) {
	log.Print(msg)
}

func main() {
	mode := flag.String("mode", "", "joiner or creator")
	wsPort := flag.Int("ws-port", 9000, "WebSocket port for browser connection")
	socksPort := flag.Int("socks-port", 1080, "SOCKS5 proxy port (joiner mode only)")
	flag.Parse()

	if *mode == "" {
		fmt.Fprintf(os.Stderr, "Usage: relay --mode joiner|creator\n")
		os.Exit(1)
	}

	cb := stdLogger{}

	switch *mode {
	case "joiner":
		log.Fatal(mobile.StartJoiner(*wsPort, *socksPort, cb))
	case "creator":
		log.Fatal(mobile.StartCreator(*wsPort, cb))
	default:
		fmt.Fprintf(os.Stderr, "Unknown mode: %s (use joiner or creator)\n", *mode)
		os.Exit(1)
	}
}
