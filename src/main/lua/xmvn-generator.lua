--
-- Copyright (c) 2023-2025 Red Hat, Inc.
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

local bsx = require "dola-bsx"

-- Run xmvn-generator
local function generate(kind)
   local deps = bsx.call1(
      "Realm:xmvn-generator",
      "org.fedoraproject.xmvn.generator.stub.GeneratorStub",
      "trampoline",
      kind
   )
   print(deps)
end

-- Post-install hook
local function os_install_post()
   local command = bsx.call0(
      "Realm:xmvn-generator",
      "org.fedoraproject.xmvn.generator.stub.CallbackStub",
      "postInstall"
   )
   print(command .. "\n")
   rpm.undefine("__os_install_post")
   print(rpm.expand("%{__os_install_post}"))
end

-- Exported module functions
return {
   generate = generate,
   os_install_post = os_install_post
}
