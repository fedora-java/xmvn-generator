--
-- Copyright (c) 2023 Red Hat, Inc.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

local lujavrite = require "lujavrite"

local libjvm = rpm.expand("%{__xmvngen_libjvm}")
local classpath = rpm.expand("%{__xmvngen_classpath}")

-- Initialize JVM
lujavrite.init(
   libjvm,
   "-Djava.class.path=" .. classpath,
   "--enable-native-access=ALL-UNNAMED"
)

-- Run xmvn-generator
local function generate(kind)
   local deps = lujavrite.call(
      "org/fedoraproject/xmvn/generator/stub/GeneratorStub", "trampoline",
      "(Ljava/lang/String;)Ljava/lang/String;",
      kind
   )
   print(deps)
end

-- Post-install hook
local function os_install_post()
   local command = lujavrite.call(
      "org/fedoraproject/xmvn/generator/stub/CallbackStub", "postInstall",
      "()Ljava/lang/String;"
   )
   print(command)
   rpm.undefine("__os_install_post")
   print(rpm.expand("%{__os_install_post}"))
end

-- Exported module functions
return {
   generate = generate,
   os_install_post = os_install_post
}
