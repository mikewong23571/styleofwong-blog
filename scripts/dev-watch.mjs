import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

function timestamp() {
  return new Date().toISOString().slice(11, 19);
}

function defaultLog(message) {
  console.log(`[${timestamp()}] ${message}`);
}

function spawnCommand(command, args, { cwd, stdio = "inherit" } = {}) {
  return spawn(command, args, { cwd, stdio });
}

function waitForExit(child, description) {
  return new Promise((resolve, reject) => {
    child.on("exit", (code, signal) => {
      if (code === 0) {
        resolve();
        return;
      }

      reject(
        new Error(
          signal
            ? `${description} exited from signal ${signal}`
            : `${description} exited with code ${code ?? "unknown"}`
        )
      );
    });

    child.on("error", (error) => {
      reject(new Error(`${description} failed to start: ${error.message}`));
    });
  });
}

export function createBuildController({
  runBuild,
  log = defaultLog,
  debounceMs = 150,
  setTimeoutImpl = setTimeout,
  clearTimeoutImpl = clearTimeout
}) {
  let timer = null;
  let buildRunning = false;
  let rerunRequested = false;
  let activeBuild = Promise.resolve();

  async function runManagedBuild({ failOnError }) {
    if (buildRunning) {
      rerunRequested = true;
      return activeBuild;
    }

    buildRunning = true;
    activeBuild = (async () => {
      let shouldRepeat;

      do {
        shouldRepeat = false;
        rerunRequested = false;
        log("build started");

        try {
          await runBuild();
          log("build passed");
        } catch (error) {
          log(`build failed: ${error.message}`);

          if (failOnError) {
            throw error;
          }
        }

        if (rerunRequested) {
          shouldRepeat = true;
        }
      } while (shouldRepeat);

      buildRunning = false;
    })();

    try {
      await activeBuild;
    } finally {
      buildRunning = false;
    }

    return activeBuild;
  }

  return {
    async runInitialBuild() {
      return runManagedBuild({ failOnError: true });
    },

    requestBuild() {
      if (timer) {
        clearTimeoutImpl(timer);
      }

      timer = setTimeoutImpl(() => {
        timer = null;
        void runManagedBuild({ failOnError: false });
      }, debounceMs);
    },

    waitForIdle() {
      return activeBuild;
    }
  };
}

function watchPaths(paths, onChange) {
  return paths.map((watchPath) =>
    fs.watch(watchPath, { recursive: true }, (_eventType, filename) => {
      onChange(watchPath, filename);
    })
  );
}

async function runBuildProcess(cwd) {
  const child = spawnCommand("npm", ["run", "build:system"], { cwd });
  await waitForExit(child, "build-system");
}

function startAstroDev(cwd) {
  return spawnCommand("npm", ["--prefix", "site/astro", "run", "dev", "--", "--host", "0.0.0.0"], {
    cwd
  });
}

async function main() {
  const cwd = process.cwd();
  const watchTargets = ["content", "metadata", "system/config"].map((target) => path.join(cwd, target));
  const controller = createBuildController({
    log: defaultLog,
    runBuild: () => runBuildProcess(cwd)
  });

  await controller.runInitialBuild();

  const astro = startAstroDev(cwd);
  const watchers = watchPaths(watchTargets, (root, filename) => {
    const changedPath = filename ? path.join(root, filename.toString()) : root;
    defaultLog(`change detected: ${path.relative(cwd, changedPath)}`);
    controller.requestBuild();
  });

  let shuttingDown = false;

  function closeWatchers() {
    for (const watcher of watchers) {
      watcher.close();
    }
  }

  function shutdown(signal) {
    if (shuttingDown) {
      return;
    }

    shuttingDown = true;
    closeWatchers();

    if (!astro.killed) {
      astro.kill(signal);
    }
  }

  process.on("SIGINT", () => shutdown("SIGINT"));
  process.on("SIGTERM", () => shutdown("SIGTERM"));

  astro.on("exit", (code, signal) => {
    closeWatchers();

    if (signal) {
      process.exitCode = 1;
      defaultLog(`astro dev exited from signal ${signal}`);
      return;
    }

    process.exitCode = code ?? 0;
  });

  astro.on("error", (error) => {
    closeWatchers();
    throw error;
  });
}

const isMainModule =
  process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isMainModule) {
  main().catch((error) => {
    console.error(error.message);
    process.exit(1);
  });
}
