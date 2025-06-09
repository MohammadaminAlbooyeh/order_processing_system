import streamlit as st
from monty_hall import simulate_game
import time


st.set_page_config(page_title="Monty Hall Simulation", page_icon="🎰", layout="centered", initial_sidebar_state="auto")
st.title(":zap: Monty Hall Simulation")
st.image("Monty_hall.png")

num_game = st.number_input("Number of simulations", 
                min_value=1, max_value=100000, value=1000, step=10)

col1, col2 = st.columns(2)


col1.subheader("Win percentage without switching")
chart1 = col1.line_chart(x=None, y=None, height=400)


col2.subheader("Win percentage with switching")
chart2 = col2.line_chart(x=None, y=None, height=400)


wins_no_switch = 0
wins_switch = 0
for i in range(num_game):
    num_wins_with_switching, num_wins_without_switching = simulate_game(1)
    wins_no_switch += num_wins_without_switching
    wins_switch += num_wins_with_switching

    chart1.add_rows({"No Switch": [wins_no_switch / (i + 1)]})
    chart2.add_rows({"Switch": [wins_switch / (i + 1)]})


    time.sleep(0.01)