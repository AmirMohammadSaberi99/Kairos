import os
import shutil
import sys
import PyInstaller.__main__

base_dir = os.path.dirname(os.path.abspath(__file__))
dist_dir = os.path.join(base_dir, "dist")
icon_ico = os.path.join(base_dir, "kairos.ico")
icon_png = os.path.join(base_dir, "kairos_logo.png")
app_py = os.path.join(base_dir, "desktop_app.py")

if not os.path.exists(dist_dir) or not os.path.exists(os.path.join(dist_dir, "index.html")):
    print("Building web assets first...")
    os.system("npm run build")

print("Starting PyInstaller packaging for Kairos...")

args = [
    app_py,
    "--name=Kairos",
    "--onefile",
    "--noconsole",
    f"--icon={icon_ico}",
    f"--add-data={dist_dir};dist",
    f"--add-data={icon_ico};.",
    f"--add-data={icon_png};.",
    "--collect-all=webview",
    "--collect-all=pythonnet",
    "--clean",
    "-y",
    f"--distpath={os.path.join(base_dir, 'release')}",
    f"--workpath={os.path.join(base_dir, 'build')}",
    f"--specpath={base_dir}",
]

PyInstaller.__main__.run(args)

exe_path = os.path.join(base_dir, "release", "Kairos.exe")
if os.path.exists(exe_path):
    size_mb = os.path.getsize(exe_path) / (1024 * 1024)
    print(f"\nSUCCESS! Kairos.exe built successfully: {exe_path} ({size_mb:.2f} MB)")
else:
    print("\nWarning: Kairos.exe was not found in release directory.")
