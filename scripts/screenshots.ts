// README screenshots, taken with android_screen_runner.
//
//   RUNNER_SCRIPTS=<runner>/scripts/emulator.sh <runner>/artifacts/android-screen-runner run scripts/screenshots.ts
//
// -> screenshots/*.webp

const APP = "com.TechHurts.CodeFile/.MainActivity";

const send = (title: string, text: string) => {
  // adb flattens raw newlines, so they travel as \n and printf turns them back.
  const encoded = text.replace(/\\/g, "\\\\").replace(/%/g, "%%").replace(/'/g, "'\\''").replace(/\n/g, "\\n");
  device.shell(
    `am start -a android.intent.action.SEND -t text/plain -n ${APP} ` +
      `--es android.intent.extra.TITLE '${title}' --es android.intent.extra.TEXT "$(printf '${encoded}')"`,
  );
  device.sleep(2500);
};

const tab = (name: string) => {
  // The tab strip scrolls sideways; the contextual MD and JSON tabs sit past the edge.
  if (!device.getButton({ desc: name }).exists()) device.swipe(950, 214, 100, 214);
  device.getButton({ desc: name }).click();
  device.sleep(600);
};

device.start();
device.install(files.latest("output/codefile/codefile-*-debug.apk"));
device.apps.clear("com.TechHurts.CodeFile");
device.launch(APP);
device.sleep(3000);

send("Inventory.java", `package demo;

import java.util.ArrayList;
import java.util.List;

/** A tiny inventory with a typed lookup. */
public final class Inventory {
  private final List<Item> items = new ArrayList<>();

  public record Item(String sku, String name, int quantity) {}

  public void add(String sku, String name, int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be >= 0");
    }
    items.add(new Item(sku, name, quantity));
  }

  public int total() {
    int sum = 0;
    for (Item item : items) {
      sum += item.quantity();
    }
    return sum;
  }
}
`);
log(device.shot("01-code-view"));

tab("Search");
log(device.shot("02-search-ribbon"));
tab("View");
log(device.shot("03-view-ribbon"));
tab("Navigation");
log(device.shot("04-navigation-ribbon"));

send("README.md", `# Release notes

CodeFile renders **Markdown** natively.

- Headings, lists and quotes
- \`inline code\` and fenced blocks
- [x] Task lists

| Format | Preview |
|--------|---------|
| Markdown | native spans |
| JSON | tree |

\`\`\`java
System.out.println("hello");
\`\`\`
`);
tab("MD");
device.getButton({ contains: "Preview" }).click();
device.sleep(1500);
log(device.shot("05-markdown-preview"));

send("config.json", `{"name":"codefile","version":26,"tags":["android","viewer"],"limits":{"tabs":20,"maxMiB":8},"offline":true,"owner":null}`);
tab("JSON");
device.getButton({ contains: "tree" }).click();
device.sleep(1500);
log(device.shot("06-json-tree"));

// A public GitHub file opened from its link: remote mode adds Save copy next to Share file.
device.keys.back(); // close the JSON tree
device.sleep(500);
device.shell(
  `am start -a android.intent.action.VIEW -n ${APP} ` +
    "-d https://github.com/maxfridbe/android_codefile/blob/main/apps/codefile/app/src/main/java/com/TechHurts/CodeFile/ApiEndpoint.java",
);
device.sleep(7000);
device.swipe(100, 214, 950, 214); // tab strip back to File (already selected; tapping it would collapse)
device.sleep(500);
device.swipe(950, 400, 100, 400); // ribbon over to the File copy group
device.sleep(800);
log(device.shot("07-github-file-copy"));

// The data tab is named for the open format: a YAML tree, then a patch file.
send("ci.yml", "name: build\non:\n  push:\n    branches: [main]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - run: make\n");
device.swipe(950, 214, 100, 214);
device.sleep(600);
device.getButton({ desc: "YAML" }).click();
device.sleep(800);
device.getButton({ desc: "View tree" }).click();
device.sleep(1500);
log(device.shot("08-yaml-tree"));
device.keys.back();
device.sleep(500);

send("fix.patch", "diff --git a/A.java b/A.java\n--- a/A.java\n+++ b/A.java\n@@ -1,3 +1,4 @@\n keep\n-old line\n+new line\n+another\n tail\n");
device.swipe(950, 214, 100, 214);
device.sleep(600);
if (!device.getButton({ desc: "View diff" }).exists()) {
  device.getButton({ desc: "Diff" }).click(); // a selected tab would collapse the ribbon, so only when needed
  device.sleep(800);
}
device.getButton({ desc: "View diff" }).click();
device.sleep(1500);
log(device.shot("09-patch-view"));
device.keys.back();
device.sleep(500);

// Share PDF asks for a paper size first.
device.swipe(100, 214, 950, 214);
device.sleep(500);
device.getButton({ desc: "File" }).click();
device.sleep(800);
device.getButton({ desc: "Share PDF" }).click();
device.sleep(1000);
device.getButton({ contains: "A4" }).click();
device.sleep(800);
log(device.shot("10-pdf-paper-size"));
