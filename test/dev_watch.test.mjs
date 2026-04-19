import assert from "node:assert/strict";
import test from "node:test";

import { createBuildController } from "../scripts/dev-watch.mjs";

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

test("build controller coalesces burst requests into one build", async () => {
  const builds = [];
  const controller = createBuildController({
    debounceMs: 10,
    log: () => {},
    runBuild: async () => {
      builds.push("build");
    }
  });

  controller.requestBuild();
  controller.requestBuild();
  controller.requestBuild();

  await delay(40);

  assert.deepEqual(builds, ["build"]);
});

test("build controller queues one follow-up build while a build is active", async () => {
  const builds = [];
  let releaseFirstBuild;
  const firstBuildDone = new Promise((resolve) => {
    releaseFirstBuild = resolve;
  });

  const controller = createBuildController({
    debounceMs: 5,
    log: () => {},
    runBuild: async () => {
      builds.push(`build-${builds.length + 1}`);

      if (builds.length === 1) {
        await firstBuildDone;
      }
    }
  });

  controller.requestBuild();
  await delay(20);
  controller.requestBuild();
  controller.requestBuild();
  controller.requestBuild();
  await delay(20);
  releaseFirstBuild();
  await delay(40);

  assert.deepEqual(builds, ["build-1", "build-2"]);
});
