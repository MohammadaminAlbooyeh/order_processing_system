import random


def monty_hall_game(switch_door):
    """_summary__

    Args:
        switch_door (bool): True if the player wants to switch the door, False otherwise.

    Returns:
        bool: True if the player wins, False otherwise.
    """


    doors = ['car', 'goat', 'goat']
    random.shuffle(doors)

    initial_choice = random.choice(range(3))

    doors_reveal = [i for i in range(3) if i!= initial_choice and doors[i] == 'goat']
    door_reveal = random.choice(doors_reveal)

    if switch_door:
        final_choice = [i for i in range(3) if i!= initial_choice and i != door_reveal][0]
    else:
        final_choice = initial_choice

    return doors[final_choice] == 'car'


def simulate_game(num_simulations):
    num_win_without_switch = sum([monty_hall_game(False) for _ in range(num_simulations)])
    num_win_with_switch = sum([monty_hall_game(True) for _ in range(num_simulations)])
    return num_win_without_switch , num_win_with_switch 


if __name__ == '__main__':
    num_simulations = 100000
    num_win_without_switch, num_win_with_switch = simulate_game(num_simulations)
    print(f"Win rate without switching door: {num_win_without_switch}")
    print(f"Win rate with switching door: {num_win_with_switch}")



