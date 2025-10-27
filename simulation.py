
class VMMSimulation:

    def __init__(self):
        self._acc = 0

    def _add_value(self, value: int):
        self._acc += value

    def _get_value(self):
        return self._acc


    def _execute_privileged_instruction(self, instruction) -> (str, bool):
        # in reality: change from user to kernel mode
        privileged_notice = "[VMM] Trapped privileged instruction"
        if instruction == 'scan_disk':
            # run disk scan
            return f"{privileged_notice} '{instruction}', emulating...", False
        if instruction == 'halt':
            return f"{privileged_notice} '{instruction}'. Halting guest.", True

    def _read_instructions_from_file(self, file_path: str) -> list[str]:
        with open(file_path, 'r', encoding='utf-8') as file:
            lines = [line.strip() for line in file.readlines()]
        return lines


    def run(self, file_path: str):
        instructions = self._read_instructions_from_file(file_path)

        for instruction in instructions:

            if instruction in ["scan_disk", "halt"]:
                output, stop = self._execute_privileged_instruction(instruction)
                print(output)
                if stop: return
                continue

            # add command
            if instruction.startswith("add"):
                splits = instruction.split(" ")
                if len(splits) > 1 and splits[1].isdigit():
                    self._add_value(int(splits[1]))
                    print(f"[Guest] Executing: {instruction}")
            elif instruction == "print":
                print(f"[Guest] Executing: {instruction}")
                print(f"Accumulator value: {self._get_value()}")


if __name__ == "__main__":
    file_path = "guest_program.txt"
    simulation = VMMSimulation()
    simulation.run(file_path)