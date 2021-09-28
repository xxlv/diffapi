#!/usr/bin/python3
# -*- coding:utf-8 -*-
import json
import os
import sys
import time

dir = "payloadsfromjson"
package = "$packageName"
GenJavaCodePath = "diffapi/src/main/java/diff/processor/payloadsfromjson/{}/{}".format(
    package, dir)
prefix = "$diffNeverUse."
TEMPLATE = """package extenal.processor.%s.%s;

import com.google.auto.service.AutoService;
import diff.Payload;
import diff.PayloadSupplier;

import java.util.ArrayList;
import java.util.List;
import static diff.utils.Log.infos;

@AutoService(PayloadSupplier.class)
public class %s implements PayloadSupplier {

     /**
     * This file is auto create by %s
     * 
     * you can change the file 'get' method 
     *
     * -------------------------------------------------------------------
     * %s 
     *
     * base on json file %s
     * -------------------------------------------------------------------
     * createTime: %s
     */
    @Override
    public Payload get() {
        infos("正在加载 [%s]（%s）的Payload");
        Payload.PayloadBuilder builder = Payload.builder();
        List<String> origin = new ArrayList<>();
        // TODO 实现构建PayLoad 
        builder.origin(origin);
        return builder.build();
    }

    @Override
    public String id() {
        return "%s";
    }

    @Override
    public String desc() {
        return "%s";
    }
}
"""


def LOG(message):
  print("[INFO] {}".format(message))


def write(file, c):
  with open(file, "w+", encoding="utf-8") as f:
    f.write(c)


def load(file):
  with open(file, "r", encoding='utf-8') as f:
    content = f.read()
  return str(content)


def process(file):
  try:
    content = load(file)
    jsonContent = dict(json.loads(content))
    nodes = ["left", "right"]

    for i in range(len(nodes)):
      node = nodes[i]
      if node in jsonContent:
        left = dict(jsonContent.get(node))
        if "id" in left:
          api = str(left.get("api"))
          api.replace(prefix, "")
          apiarr = api.split(".")
          clazz = []
          for i in range(len(apiarr)):
            clazz.append(str(apiarr[i][0]).upper() + apiarr[i][1:])
          clazz = clazz[1:]
          className = "_".join(clazz)

          id = left.get("id")
          desc = jsonContent.get("desc", "")
          writeToPath = "{}{}{}.java".format(GenJavaCodePath, "\\", className)
          write(writeToPath, TEMPLATE % (package,
                                         dir, className,
                                         sys.argv[0].split("/")[-1], desc,
                                         str(file).split("\\")[-1], time.time(),
                                         desc,
                                         api, id,
                                         desc))
          LOG("正在写入 {}".format(writeToPath))

  except Exception as e:
    pass


def process_file(path, func):
  for f in os.listdir(path):
    f = os.path.join(path, f)
    if os.path.isfile(f):
      func(f)
    else:
      process_file(f, func)


def genJavaFile(jsonpath):
  process_file(jsonpath, process)
  LOG("正在处理 {}".format(jsonpath))


if __name__ == '__main__':
  genJavaFile("../checkpoints")
