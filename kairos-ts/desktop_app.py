import argparse
import http.server
import os
import socket
import socketserver
import sys
import threading
import time

try:
    import webview
except ImportError:
    webview = None

DEFAULT_PORT = 58942


def get_base_dir():
    if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
        return sys._MEIPASS
    return os.path.dirname(os.path.abspath(__file__))


def is_port_free(port: int) -> bool:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind(("127.0.0.1", port))
            return True
    except OSError:
        return False


def find_kairos_port(preferred_port: int = DEFAULT_PORT) -> int:
    # 1. Try preferred port
    if is_port_free(preferred_port):
        return preferred_port

    # 2. Try nearby dedicated ports in Kairos range
    for p in range(preferred_port + 1, preferred_port + 30):
        if is_port_free(p):
            return p

    # 3. Fallback to OS assigned ephemeral port
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def start_local_server(dist_dir: str, port: int):
    class QuietHandler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=dist_dir, **kwargs)

        def log_message(self, format, *args):
            pass  # Suppress console log spam

    server = socketserver.TCPServer(("127.0.0.1", port), QuietHandler)
    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    return server


def main():
    parser = argparse.ArgumentParser(description="Kairos Desktop App")
    parser.add_argument(
        "--port",
        type=int,
        default=int(os.environ.get("KAIROS_PORT", DEFAULT_PORT)),
        help="Preferred port for Kairos local server",
    )
    args, _ = parser.parse_known_args()

    base_dir = get_base_dir()
    dist_dir = os.path.join(base_dir, "dist")
    index_html = os.path.join(dist_dir, "index.html")

    # If running from source and dist not present, build it
    if not os.path.exists(index_html):
        print("Building Kairos production bundle...")
        os.system("npm run build")

    if not os.path.exists(index_html):
        print(f"Error: Could not find '{index_html}'")
        sys.exit(1)

    port = find_kairos_port(args.port)
    server = start_local_server(dist_dir, port)
    app_url = f"http://127.0.0.1:{port}/index.html"

    print(f"Kairos Server active at: {app_url} (Port: {port})")

    # Locate icon
    ico_path = os.path.join(base_dir, "kairos.ico")
    png_path = os.path.join(base_dir, "kairos_logo.png")
    app_icon = ico_path if os.path.exists(ico_path) else (png_path if os.path.exists(png_path) else None)

    if webview:
        window = webview.create_window(
            title="Kairos — Minimalist Daily Planner & Focus",
            url=app_url,
            width=1160,
            height=780,
            min_size=(940, 640),
            background_color="#090B10",
        )
        try:
            webview.start(icon=app_icon)
        finally:
            server.shutdown()
    else:
        import webbrowser
        webbrowser.open(app_url)
        print("Webview not found. Running in default browser.")
        print("Press Ctrl+C to stop the server.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            server.shutdown()


if __name__ == "__main__":
    main()
