/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlin.komposite.processor

import net.akehurst.language.agl.processor.FormatResultDefault
import net.akehurst.language.agl.processor.FormatterAbstract
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.processor.FormatResult
import net.akehurst.language.api.processor.LanguageProcessorPhase

class Formatter : FormatterAbstract<Asm>() {

    override fun format(asm: Asm): FormatResult {
        return FormatResultDefault("", IssueHolder(LanguageProcessorPhase.FORMAT))
    }

}