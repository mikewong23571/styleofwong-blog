import { visit } from "unist-util-visit";

export function remarkMermaidCode() {
  return (tree) => {
    visit(tree, "code", (node, index, parent) => {
      if (!parent || node.lang !== "mermaid") {
        return;
      }

      parent.children[index] = {
        type: "mdxJsxFlowElement",
        name: "Mermaid",
        attributes: [
          {
            type: "mdxJsxAttribute",
            name: "code",
            value: node.value
          }
        ],
        children: []
      };
    });
  };
}
