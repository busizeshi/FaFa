from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import base64

FILES = {
    "/snowball.png": Path(r"D:\dev\Java\FaFa\design-assets\pets\hero-snowball-3d.png"),
    "/xiaoman.png": Path(r"D:\dev\Java\FaFa\design-assets\pets\hero-xiaoman-3d.png"),
}


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        target = self.path[:-4] + ".png" if self.path.endswith(".b64") else self.path
        path = FILES.get(target)
        if path is None or not path.is_file():
            self.send_response(404)
            self.end_headers()
            return
        data = path.read_bytes()
        self.send_response(200)
        is_base64 = self.path.endswith(".b64")
        if is_base64:
            data = base64.b64encode(data)
        self.send_header("Content-Type", "text/plain" if is_base64 else "image/png")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(data)

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.end_headers()

    def log_message(self, fmt, *args):
        return


ThreadingHTTPServer(("0.0.0.0", 8765), Handler).serve_forever()
