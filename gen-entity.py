#!/usr/bin/env python
# 从 DDL 生成 freecrm 实体类（简单场景）
import io, re, sys

DDL = io.open(sys.argv[1], encoding='utf-8').read()
OUT = sys.argv[2]

tables = re.findall(r"CREATE TABLE `(\w+)` \((.*?)\)\s*ENGINE", DDL, re.S)
SKIP_COLS = {'create_time', 'update_time', 'creator', 'deleted'}

def camel(col):
    return ''.join(w.capitalize() if i else w for i, w in enumerate(col.split('_')))

def type_map(t):
    t = t.lower()
    if 'bigint' in t: return 'Long'
    if 'tinyint' in t or 'int' in t: return 'Integer'
    if 'decimal' in t: return 'java.math.BigDecimal'
    if 'datetime' in t: return 'java.time.LocalDateTime'
    if 'date' in t: return 'java.time.LocalDate'
    return 'String'

for name, body in tables:
    cls = ''.join(w.capitalize() for w in name.split('_'))
    lines = [f"package com.freecrm.crm.entity;", "",
             "import com.baomidou.mybatisplus.annotation.TableName;",
             "import com.freecrm.common.entity.BaseEntity;",
             "import lombok.Data;",
             "import lombok.EqualsAndHashCode;", ""]
    need_date = False
    fields = []
    for m in re.finditer(r"`(\w+)` (\w+[^,]*?)(?: COMMENT '([^']*)')?,", body):
        col, sqltype, comment = m.group(1), m.group(2), m.group(3) or ''
        if col == 'id' or col in SKIP_COLS:
            continue
        jt = type_map(sqltype)
        fields.append((camel(col), jt, comment))
    imports = sorted({t for _, t, _ in fields if t.startswith('java.')})
    for imp in imports:
        lines.append(f"import {imp};")
    if imports:
        lines.append("")
    lines.append(f"/** {''.join(re.findall(r'COMMENT=.(.+?).$', '')) or name} */")
    lines.append("@Data")
    lines.append("@EqualsAndHashCode(callSuper = true)")
    lines.append(f'@TableName("{name}")')
    lines.append(f"public class {cls} extends BaseEntity {{")
    lines.append("")
    for fname, jt, comment in fields:
        if comment:
            lines.append(f"    /** {comment} */")
        lines.append(f"    private {jt} {fname};")
    lines.append("}")
    io.open(f"{OUT}/{cls}.java", 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print(cls, len(fields))
