"""Renders a text log as a terminal-style PNG 'screenshot' (used for the output-screenshots folder)."""
import sys, textwrap
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

def render(text, path, title):
    lines = text.rstrip("\n").split("\n")
    width = max(60, min(max(len(l) for l in lines), 150))
    fig_w = width * 0.093 + 0.8
    fig_h = (len(lines) + 3) * 0.2 + 0.6
    fig = plt.figure(figsize=(fig_w, fig_h), facecolor="#1e1e1e")
    fig.text(0.012, 1 - 0.28 / fig_h, "●", color="#ff5f56", fontsize=11, va="center")
    fig.text(0.030, 1 - 0.28 / fig_h, "●", color="#ffbd2e", fontsize=11, va="center")
    fig.text(0.048, 1 - 0.28 / fig_h, "●", color="#27c93f", fontsize=11, va="center")
    fig.text(0.5, 1 - 0.28 / fig_h, title, color="#aaaaaa", fontsize=9, ha="center", va="center", family="monospace")
    y = 1 - 0.62 / fig_h
    for l in lines:
        color = "#d4d4d4"
        if "[OK]" in l: color = "#6ee06e"
        elif "[BLOCKED]" in l or l.strip().startswith("->"): color = "#ffb454"
        elif l.startswith("####") or l.startswith("=="): color = "#61afef"
        elif "NO OVERBOOKING" in l or "DEMO COMPLETE" in l: color = "#6ee06e"
        fig.text(0.012, y, l, color=color, fontsize=8.3, family="monospace", va="top")
        y -= 0.2 / fig_h
    fig.savefig(path, dpi=110, facecolor=fig.get_facecolor())
    plt.close(fig)

if __name__ == "__main__":
    src, outdir = sys.argv[1], sys.argv[2]
    text = open(src).read()
    parts = text.split("\n########## ")
    head, steps = parts[0], ["########## " + p for p in parts[1:]]
    groups = {"01_java_master_data_and_schedule": steps[0:3], "02_java_booking_cancellation": steps[3:5],
              "03_java_reports": steps[5:6], "04_java_concurrency_and_export": steps[6:]}
    for name, g in groups.items():
        render("\n".join(g), f"{outdir}/{name}.png", f"java -cp out:lib/... Main --demo   [{name}]")
        print("rendered", name)
