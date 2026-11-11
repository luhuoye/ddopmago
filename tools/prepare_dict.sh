#!/usr/bin/env bash
# 下载 https://github.com/luhuoye/-2008-.git 的两个分卷压缩包 → 合并解压出 mdx/mdd →
# 重新打成 zip，并切成每卷 ≤24MB 的分卷 app/src/main/assets/dict/dict.zip.00,01…
# 若仓库里存在 dictsrc/ 目录（自己放的分卷），则优先使用它，不联网。
set -euo pipefail
OUT=app/src/main/assets/dict
if ls "$OUT"/dict.zip.* >/dev/null 2>&1; then echo "词典分卷已存在，跳过"; exit 0; fi
W=$(mktemp -d)
if [ -d dictsrc ] && [ -n "$(ls -A dictsrc)" ]; then SRC=dictsrc
else git clone --depth 1 https://github.com/luhuoye/-2008-.git "$W/src"; SRC="$W/src"; fi
echo "== 源文件 =="; find "$SRC" -path '*/.git' -prune -o -type f -printf '%s\t%p\n'
mkdir -p "$W/ex"
# 1) 7z 自动识别并合并 .7z.001 / .zip.001 / .z01+.zip / .part1.rar 等分卷
while IFS= read -r f; do 7z x -y -o"$W/ex" "$f" >/dev/null 2>&1 || true
done < <(find "$SRC" -path '*/.git' -prune -o -type f \( -iname '*.001' -o -iname '*.7z' -o -iname '*.zip' -o -iname '*.rar' \) -print | sort)
# 2) 若没有成功：按文件名顺序把分卷直接拼接再解压
if [ -z "$(find "$W/ex" "$SRC" -path '*/.git' -prune -o -iname '*.mdx' -print -quit)" ]; then
  cat $(find "$SRC" -path '*/.git' -prune -o -type f -print | grep -Ei '\.(7z|zip|rar)\.?[0-9]+$|\.[0-9]{3}$|\.part[0-9]+' | sort) > "$W/merged.bin" || true
  7z x -y -o"$W/ex" "$W/merged.bin" >/dev/null 2>&1 || true
fi
mkdir -p "$W/pack"
find "$W/ex" "$SRC" -path '*/.git' -prune -o -type f \( -iname '*.mdx' -o -iname '*.mdd' \) -print | while IFS= read -r f; do cp -n "$f" "$W/pack/"; done
if [ -z "$(ls -A "$W/pack")" ]; then echo "没有找到 mdx/mdd，请检查上面的源文件列表"; exit 1; fi
ls -la "$W/pack"
(cd "$W/pack" && zip -9 -q "$W/dict.zip" ./*)
mkdir -p "$OUT"
split -b 24000000 -d -a 2 "$W/dict.zip" "$OUT/dict.zip."
ls -la "$OUT"
