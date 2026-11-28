"""Boot a packaged Frenchies jar on a Minecraft hotfix and summon its entity."""

import argparse
import asyncio
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
from urllib.request import urlretrieve


async def check_server(server, launch, minecraft):
    process = await asyncio.create_subprocess_exec(
        "java", "-Xmx2G", *launch, "nogui", cwd=server, stdin=asyncio.subprocess.PIPE,
        stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.STDOUT,
    )
    version_seen = False
    summoned = False
    try:
        async with asyncio.timeout(240):
            while line := await process.stdout.readline():
                message = line.decode("utf-8", errors="replace").rstrip()
                print(message, flush=True)
                if message.endswith(f"Starting minecraft server version {minecraft}"):
                    version_seen = True
                if "Done (" in message and 'For help, type "help"' in message:
                    process.stdin.write(b"summon frenchies:frenchie\n")
                    await process.stdin.drain()
                elif "Summoned new Frenchie" in message:
                    summoned = True
                    process.stdin.write(b"stop\n")
                    await process.stdin.drain()
            if await process.wait() or not version_seen or not summoned:
                raise SystemExit(f"Minecraft {minecraft} did not complete the Frenchie summon check")
    finally:
        if process.returncode is None:
            process.kill()
            await process.wait()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("minecraft")
    parser.add_argument("loader", choices=["fabric", "neoforge"])
    parser.add_argument("loader_version")
    parser.add_argument("--fabric-api")
    args = parser.parse_args()
    if args.loader == "fabric" and not args.fabric_api:
        parser.error("Fabric requires --fabric-api")

    with tempfile.TemporaryDirectory(prefix="frenchies-patch-") as folder:
        server = Path(folder)
        mods = server / "mods"
        mods.mkdir()
        shutil.copy2(args.jar, mods)
        if args.loader == "fabric":
            launcher = f"https://meta.fabricmc.net/v2/versions/loader/{args.minecraft}/{args.loader_version}/1.1.2/server/jar"
            urlretrieve(launcher, server / "launcher.jar")
            api = f"fabric-api-{args.fabric_api}.jar"
            urlretrieve(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{args.fabric_api}/{api}", mods / api)
            launch = ["-jar", "launcher.jar"]
        else:
            version = args.loader_version
            artifact = f"https://maven.neoforged.net/releases/net/neoforged/neoforge/{version}/neoforge-{version}-installer.jar"
            urlretrieve(artifact, server / "installer.jar")
            subprocess.run(["java", "-jar", "installer.jar", "--installServer"], cwd=server, check=True, timeout=480)
            platform = "win" if os.name == "nt" else "unix"
            launch = [f"@libraries/net/neoforged/neoforge/{version}/{platform}_args.txt"]

        (server / "eula.txt").write_text("eula=true\n")
        (server / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=0\nview-distance=3\n")
        asyncio.run(check_server(server, launch, args.minecraft))


if __name__ == "__main__":
    main()
